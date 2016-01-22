/*
 * Copyright (C) 2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.aamas16;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verifyNotNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import org.optaplanner.core.config.solver.SolverConfig;

import com.github.rinde.logistics.pdptw.mas.TruckFactory.DefaultTruckFactory;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionStopConditions;
import com.github.rinde.logistics.pdptw.mas.comm.DoubleBid;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder;
import com.github.rinde.logistics.pdptw.mas.route.RtSolverRoutePlanner;
import com.github.rinde.logistics.pdptw.solver.CheapestInsertionHeuristic;
import com.github.rinde.logistics.pdptw.solver.Opt2;
import com.github.rinde.logistics.pdptw.solver.optaplanner.OptaplannerSolvers;
import com.github.rinde.rinsim.central.Central;
import com.github.rinde.rinsim.central.rt.RealtimeSolver;
import com.github.rinde.rinsim.central.rt.RtCentral;
import com.github.rinde.rinsim.central.rt.RtSolverModel;
import com.github.rinde.rinsim.central.rt.RtSolverPanel;
import com.github.rinde.rinsim.central.rt.SolverToRealtimeAdapter;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.time.MeasuredDeviation;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger.LogEntry;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.experiment.CommandLineProgress;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.Experiment.Builder;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.experiment.PostProcessor;
import com.github.rinde.rinsim.experiment.PostProcessors;
import com.github.rinde.rinsim.io.FileProvider;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.pdptw.common.RouteRenderer;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.TimeLinePanel;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioConverters;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import net.openhft.affinity.AffinityLock;

/**
 *
 * @author Rinde van Lon
 */
public class PerformExperiment {
  static final String VANLON_HOLVOET_DATASET = "files/vanLonHolvoet15/";
  static final String GENDREAU_DATASET = "files/gendreau2006/requests";
  static final String RESULTS_MAIN_DIR = "files/results/";

  enum ExperimentType {
    GENDREAU(Gendreau06ObjectiveFunction.instance()) {
      @Override
      void apply(Builder bldr) {
        bldr.addScenarios(
          FileProvider.builder().add(Paths.get(GENDREAU_DATASET))
              .filter("glob:**req_rapide_**"))
            .setScenarioReader(
              Functions.compose(ScenarioConverter.TO_ONLINE_250,
                Gendreau06Parser.reader()))
            .addResultListener(new GendreauResultWriter(experimentDir));;
      }
    },

    GENDREAU_OFFLINE(Gendreau06ObjectiveFunction.instance()) {
      @Override
      void apply(Builder bldr) {
        bldr.addScenarios(
          FileProvider.builder().add(Paths.get(GENDREAU_DATASET))
              .filter("glob:**req_rapide_**"))
            .setScenarioReader(Functions.compose(ScenarioConverter.TO_OFFLINE,
              Gendreau06Parser.reader()))
            .addResultListener(new GendreauResultWriter(experimentDir));
      }
    },

    /**
     * Experiment using the Van Lon & Holvoet (2015) dataset.
     */
    VAN_LON15(Gendreau06ObjectiveFunction.instance(50d)) {
      @Override
      void apply(Builder bldr) {
        bldr.addScenarios(FileProvider.builder()
            .add(Paths.get(VANLON_HOLVOET_DATASET)).filter("glob:**[0-9].scen"))
            .setScenarioReader(
              ScenarioIO.readerAdapter(ScenarioConverter.TO_ONLINE_250))
            .addResultListener(new VanLonHolvoetResultWriter(experimentDir));
      }
    },

    /**
     * Investigate one setting of the Van Lon & Holvoet (2015) dataset with many
     * repetitions.
     */
    TIME_DEVIATION(Gendreau06ObjectiveFunction.instance(50d)) {
      @Override
      void apply(Builder bldr) {
        bldr.addScenarios(
          FileProvider.builder().add(Paths.get(VANLON_HOLVOET_DATASET))
              .filter("glob:**0.50-20-10.00-[0-9].scen"))
            .setScenarioReader(
              ScenarioIO.readerAdapter(ScenarioConverter.TO_ONLINE_250))
            .repeat(10)
            .addResultListener(new VanLonHolvoetResultWriter(experimentDir));
      }
    };

    final File experimentDir = new File(RESULTS_MAIN_DIR + "/" + name());

    private final Gendreau06ObjectiveFunction objectiveFunction;

    ExperimentType(Gendreau06ObjectiveFunction objFunc) {
      objectiveFunction = objFunc;
    }

    abstract void apply(Experiment.Builder b);

    public Gendreau06ObjectiveFunction getObjectiveFunction() {
      return objectiveFunction;
    }

    static ExperimentType find(String string) {
      for (final ExperimentType type : ExperimentType.values()) {
        final String name = type.name();
        if (string.equalsIgnoreCase(name)
            || string.equalsIgnoreCase(name.replace("_", ""))) {
          return type;
        }
      }
      throw new IllegalArgumentException(
          ExperimentType.class.getName() + " has no value called " + string);
    }
  }

  public static void main(String[] args) throws IOException {
    checkArgument(args.length > 2 && args[0].equals("-exp"),
      "The type of experiment that should be run must be specified as follows: "
          + "\'-exp vanlon15|gendreau|timedeviation\', this option must be the "
          + "first in the list.");

    final ExperimentType experimentType = ExperimentType.find(args[1]);
    System.out.println(experimentType);
    final String[] expArgs = new String[args.length - 2];
    System.arraycopy(args, 2, expArgs, 0, args.length - 2);

    final Gendreau06ObjectiveFunction objFunc =
      experimentType.getObjectiveFunction();

    final StochasticSupplier<RealtimeSolver> cih =
      SolverToRealtimeAdapter
          .create(CheapestInsertionHeuristic.supplier(objFunc));

    final StochasticSupplier<RealtimeSolver> opt2 = Opt2.builder()
        .withObjectiveFunction(objFunc)
        .buildRealtimeSolverSupplier();

    final long time = System.currentTimeMillis();
    final Experiment.Builder experimentBuilder = Experiment
        .build(objFunc)
        .computeLocal()
        .withRandomSeed(123)
        .withThreads(11)
        .repeat(1)
        .addResultListener(new CommandLineProgress(System.out))
        .usePostProcessor(LogProcessor.INSTANCE);

    experimentType.apply(experimentBuilder);

    experimentBuilder
        .addConfiguration(MASConfiguration.pdptwBuilder()
            .setName("ReAuction-2optRP-cihBID")
            .addEventHandler(AddVehicleEvent.class,
              DefaultTruckFactory.builder()
                  .setRoutePlanner(RtSolverRoutePlanner.supplier(opt2))
                  .setCommunicator(RtSolverBidder.supplier(objFunc, cih,
                    RtSolverBidder.BidFunctions.PLAIN))
                  .setLazyComputation(false)
                  .setRouteAdjuster(RouteFollowingVehicle.delayAdjuster())
                  .build())
            .addModel(AuctionCommModel.builder(DoubleBid.class)
                .withStopCondition(
                  AuctionStopConditions.and(
                    AuctionStopConditions.<DoubleBid>atLeastNumBids(2),
                    AuctionStopConditions.<DoubleBid>or(
                      AuctionStopConditions.<DoubleBid>allBidders(),
                      AuctionStopConditions
                          .<DoubleBid>maxAuctionDuration(5000)))))
            .addModel(RtSolverModel.builder()
                .withThreadPoolSize(3)
                .withThreadGrouping(true))
            .addModel(RealtimeClockLogger.builder())
            .build())
        .addConfiguration(MASConfiguration.pdptwBuilder()
            .setName("ReAuction-2optRP-cihBID-BAL-HIGH")
            .addEventHandler(AddVehicleEvent.class,
              DefaultTruckFactory.builder()
                  .setRoutePlanner(RtSolverRoutePlanner.supplier(opt2))
                  .setCommunicator(RtSolverBidder.supplier(objFunc, cih,
                    RtSolverBidder.BidFunctions.BALANCED_HIGH))
                  .setLazyComputation(false)
                  .setRouteAdjuster(RouteFollowingVehicle.delayAdjuster())
                  .build())
            .addModel(AuctionCommModel.builder(DoubleBid.class)
                .withStopCondition(
                  AuctionStopConditions.and(
                    AuctionStopConditions.<DoubleBid>atLeastNumBids(2),
                    AuctionStopConditions.or(
                      AuctionStopConditions.<DoubleBid>allBidders(),
                      AuctionStopConditions
                          .<DoubleBid>maxAuctionDuration(5000)))))
            .addModel(RtSolverModel.builder()
                .withThreadPoolSize(3)
                .withThreadGrouping(true))
            .addModel(RealtimeClockLogger.builder())
            .build())
        .addConfiguration(MASConfiguration.pdptwBuilder()
            .setName("ReAuction-2optRP-cihBID-BAL-LOW")
            .addEventHandler(AddVehicleEvent.class,
              DefaultTruckFactory.builder()
                  .setRoutePlanner(RtSolverRoutePlanner.supplier(opt2))
                  .setCommunicator(RtSolverBidder.supplier(objFunc, cih,
                    RtSolverBidder.BidFunctions.BALANCED_LOW))
                  .setLazyComputation(false)
                  .setRouteAdjuster(RouteFollowingVehicle.delayAdjuster())
                  .build())
            .addModel(AuctionCommModel.builder(DoubleBid.class)
                .withStopCondition(
                  AuctionStopConditions.and(
                    AuctionStopConditions.<DoubleBid>atLeastNumBids(2),
                    AuctionStopConditions.or(
                      AuctionStopConditions.<DoubleBid>allBidders(),
                      AuctionStopConditions
                          .<DoubleBid>maxAuctionDuration(5000)))))
            .addModel(RtSolverModel.builder()
                .withThreadPoolSize(3)
                .withThreadGrouping(true))
            .addModel(RealtimeClockLogger.builder())
            .build())

        // cheapest insertion
        .addConfiguration(MASConfiguration.builder(
          RtCentral.solverConfigurationAdapt(
            CheapestInsertionHeuristic.supplier(objFunc), "", true))
            .addModel(RealtimeClockLogger.builder())
            .build())

        // 2-opt cheapest insertion
        .addConfiguration(MASConfiguration.builder(
          RtCentral.solverConfiguration(
            Opt2.builder()
                .withObjectiveFunction(objFunc)
                .buildRealtimeSolverSupplier(),
            ""))
            .addModel(RealtimeClockLogger.builder())
            .build())

        .addConfiguration(MASConfiguration.builder(
          Central.solverConfiguration(
            CheapestInsertionHeuristic.supplier(objFunc)))
            .build())

        .addConfiguration(MASConfiguration.builder(
          Central.solverConfiguration(
            Opt2.builder()
                .withObjectiveFunction(objFunc)
                .buildSolverSupplier()))
            .build());

    // .addConfiguration(
    // Central.solverConfiguration(
    // OptaplannerSolvers.builder()
    // .withUnimprovedMsLimit(1000L)
    // .withObjectiveFunction(
    // Gendreau06ObjectiveFunction.instance(30d))
    // .withValidated(true)
    // .buildSolver()));

    final Map<String, SolverConfig> configs =
      OptaplannerSolvers.getConfigsFromBenchmark(
        "com/github/rinde/jaamas16/benchmarkConfig.xml");

    for (final Entry<String, SolverConfig> config : configs.entrySet()) {
      experimentBuilder.addConfiguration(
        MASConfiguration.pdptwBuilder()
            .addModel(
              RtCentral.builder(
                OptaplannerSolvers.builder()
                    .withUnimprovedMsLimit(10000L)
                    .withSolverConfig(config.getValue())
                    .withName(config.getKey())
                    .buildRealtimeSolverSupplier())
                  .withContinuousUpdates(true))
            .addModel(RealtimeClockLogger.builder())
            .addEventHandler(AddVehicleEvent.class, RtCentral.vehicleHandler())
            .setName(config.getKey())
            .build());
    }

    experimentBuilder.showGui(View.builder()
        .withAutoPlay()
        .withAutoClose()
        .withSpeedUp(8)
        // .withFullScreen()
        .withTitleAppendix("AAMAS 2016 Experiment")
        .with(RoadUserRenderer.builder()
            .withToStringLabel())
        .with(RouteRenderer.builder())
        .with(PDPModelRenderer.builder())
        .with(PlaneRoadModelRenderer.builder())
        // .with(AuctionPanel.builder())
        .with(TimeLinePanel.builder())
        .with(RtSolverPanel.builder())
        .withResolution(1280, 1024));

    final Optional<ExperimentResults> results =
      experimentBuilder.perform(System.out, expArgs);
    final long duration = System.currentTimeMillis() - time;
    if (!results.isPresent()) {
      return;
    }

    System.out.println("Done, computed " + results.get().getResults().size()
        + " simulations in " + duration / 1000d + "s");

  }

  enum ScenarioConverter implements Function<Scenario, Scenario> {
    /**
     * Changes ticksize to 250ms and adds stopcondition with maximum sim time of
     * 10 hours.
     */
    TO_ONLINE_250 {
      @Override
      public Scenario apply(@Nullable Scenario input) {
        final Scenario s = verifyNotNull(input);
        return Scenario.builder(s)
            .removeModelsOfType(TimeModel.AbstractBuilder.class)
            .addModel(TimeModel.builder().withTickLength(250).withRealTime())
            .setStopCondition(StopConditions.or(s.getStopCondition(),
              StopConditions.limitedTime(10 * 60 * 60 * 1000)))
            .build();
      }
    },
    TO_OFFLINE {
      @Override
      public Scenario apply(Scenario input) {
        final Scenario s = ScenarioConverters
            .eventConverter(new Function<TimedEvent, TimedEvent>() {
          @Override
          public TimedEvent apply(TimedEvent input) {
            if (input instanceof AddParcelEvent) {
              return AddParcelEvent.create(
                Parcel.builder(((AddParcelEvent) input).getParcelDTO())
                    .orderAnnounceTime(-1)
                    .buildDTO());
            }
            return input;
          }
        }).apply(input);
        return Scenario.builder(s)
            .removeModelsOfType(TimeModel.AbstractBuilder.class)
            .addModel(TimeModel.builder().withTickLength(250))
            .setStopCondition(StopConditions.or(s.getStopCondition(),
              StopConditions.limitedTime(10 * 60 * 60 * 1000)))
            .build();
      }
    }
  }

  @AutoValue
  abstract static class AuctionStats {
    abstract int getNumParcels();

    abstract int getNumReauctions();

    abstract int getNumUnsuccesfulReauctions();

    abstract int getNumFailedReauctions();

    static AuctionStats create(int numP, int numR, int numUn, int numF) {
      return new AutoValue_PerformExperiment_AuctionStats(numP, numR, numUn,
          numF);
    }
  }

  @AutoValue
  abstract static class ExperimentInfo {
    abstract List<LogEntry> getLog();

    abstract long getRtCount();

    abstract long getStCount();

    abstract StatisticsDTO getStats();

    abstract ImmutableList<MeasuredDeviation> getMeasuredDeviations();

    abstract Optional<AuctionStats> getAuctionStats();

    static ExperimentInfo create(List<LogEntry> log, long rt, long st,
        StatisticsDTO stats, ImmutableList<MeasuredDeviation> dev,
        Optional<AuctionStats> aStats) {
      return new AutoValue_PerformExperiment_ExperimentInfo(log, rt, st, stats,
          dev, aStats);
    }
  }

  enum LogProcessor implements PostProcessor<ExperimentInfo> {
    INSTANCE {
      @Override
      public ExperimentInfo collectResults(Simulator sim, SimArgs args) {

        @Nullable
        final RealtimeClockLogger logger =
          sim.getModelProvider().tryGetModel(RealtimeClockLogger.class);

        @Nullable
        final AuctionCommModel<?> auctionModel =
          sim.getModelProvider().tryGetModel(AuctionCommModel.class);

        final Optional<AuctionStats> aStats;
        if (auctionModel == null) {
          aStats = Optional.absent();
        } else {
          final int parcels = auctionModel.getNumParcels();
          final int reauctions = auctionModel.getNumAuctions() - parcels;
          final int unsuccessful = auctionModel.getNumUnsuccesfulAuctions();
          final int failed = auctionModel.getNumFailedAuctions();
          aStats = Optional.of(
            AuctionStats.create(parcels, reauctions, unsuccessful, failed));
        }

        final StatisticsDTO stats =
          PostProcessors.statisticsPostProcessor().collectResults(sim, args);

        System.out.println("success: " + args);

        if (logger == null) {
          return ExperimentInfo.create(new ArrayList<LogEntry>(),
            0,
            sim.getCurrentTime() / sim.getTimeStep(),
            stats,
            ImmutableList.<MeasuredDeviation>of(),
            aStats);
        }
        return ExperimentInfo.create(logger.getLog(), logger.getRtCount(),
          logger.getStCount(), stats, logger.getDeviations(), aStats);
      }

      @Override
      public FailureStrategy handleFailure(Exception e, Simulator sim,
          SimArgs args) {

        System.out.println("Fail: " + args);
        e.printStackTrace();
        System.out.println(AffinityLock.dumpLocks());
        // System.out.println(Joiner.on("\n").join(
        // sim.getModelProvider().getModel(RealtimeClockLogger.class).getLog()));
        // System.out.println("RETRY!");
        return FailureStrategy.RETRY;
      }

    }
  }

  static class DebugParcelCreator
      implements TimedEventHandler<AddParcelEvent>, Serializable {

    private static final long serialVersionUID = -3604876394924095797L;
    Map<SimulatorAPI, AtomicLong> map;

    DebugParcelCreator() {
      map = new ConcurrentHashMap<>();
    }

    @Override
    public void handleTimedEvent(AddParcelEvent event, SimulatorAPI simulator) {

      if (!map.containsKey(simulator)) {
        map.put(simulator, new AtomicLong());
      }
      final String str =
        "p" + Long.toString(map.get(simulator).getAndIncrement());

      simulator.register(new Parcel(event.getParcelDTO()) {
        @Override
        public String toString() {
          return str;
        }
      });

    }

  }
}
