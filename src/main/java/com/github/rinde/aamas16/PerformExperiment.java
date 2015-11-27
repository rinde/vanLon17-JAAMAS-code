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
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import org.joda.time.format.ISODateTimeFormat;

import com.github.rinde.logistics.pdptw.mas.TruckFactory.DefaultTruckFactory;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionStopConditions;
import com.github.rinde.logistics.pdptw.mas.comm.DoubleBid;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder;
import com.github.rinde.logistics.pdptw.mas.route.RtSolverRoutePlanner;
import com.github.rinde.logistics.pdptw.solver.CheapestInsertionHeuristic;
import com.github.rinde.logistics.pdptw.solver.Opt2;
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
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.experiment.PostProcessor;
import com.github.rinde.rinsim.experiment.PostProcessor.FailureStrategy;
import com.github.rinde.rinsim.experiment.PostProcessors;
import com.github.rinde.rinsim.io.FileProvider;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.pdptw.common.RouteRenderer;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.TimeLinePanel;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.scenario.gendreau06.GendreauProblemClass;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.auto.value.AutoValue;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import net.openhft.affinity.AffinityLock;

/**
 *
 * @author Rinde van Lon
 */
public class PerformExperiment {
  static final String VANLON_HOLVOET_DATASET = "files/vanLonHolvoet15/";
  static final String RESULTS_MAIN_DIR = "files/results/";

  enum ExperimentType {

    GENDREAU(Gendreau06ObjectiveFunction.instance()) {
      @Override
      void apply(Builder bldr) {
        bldr.addScenarios(
          FileProvider.builder()
              .add(Paths.get("files/gendreau2006/requests"))
              .filter("glob:**req_rapide_**"))
            .setScenarioReader(Functions.compose(ScenarioConverter.INSTANCE,
              Gendreau06Parser.reader()))
            .addResultListener(new GendreauResultWriter(experimentDir));;
      }
    },

    /**
     * Experiment on the Van Lon & Holvoet (2015) dataset.
     */
    VAN_LON15(Gendreau06ObjectiveFunction.instance(50d)) {
      @Override
      void apply(Builder bldr) {
        bldr.addScenarios(
          FileProvider.builder()
              .add(Paths.get(VANLON_HOLVOET_DATASET))
              .filter("glob:**[0-9].scen"))
            .setScenarioReader(
              ScenarioIO.readerAdapter(ScenarioConverter.INSTANCE))
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
        bldr.addScenarios(FileProvider.builder()
            .add(Paths.get(VANLON_HOLVOET_DATASET))
            .filter("glob:**0.50-20-10.00-[0-9].scen"))
            .setScenarioReader(
              ScenarioIO.readerAdapter(ScenarioConverter.INSTANCE))
            .repeat(10)
            .addResultListener(new VanLonHolvoetResultWriter(experimentDir));
      }
    };

    final File experimentDir =
      createExperimentDir(
        new File(RESULTS_MAIN_DIR + "/" + name()));

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

    if (experimentType == ExperimentType.GENDREAU) {
      MeasureGendreau.read(new File(MeasureGendreau.PROPS_FILE));
    }

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
        .withThreads(1)
        .repeat(1)
        .addResultListener(new CommandLineProgress(System.out));

    experimentType.apply(experimentBuilder);

    experimentBuilder
        .usePostProcessor(LogProcessor.INSTANCE)
        .addConfiguration(MASConfiguration.pdptwBuilder()
            .setName("ReAuction-2optRP-cihBID")
            .addEventHandler(AddVehicleEvent.class,
              DefaultTruckFactory.builder()
                  .setRoutePlanner(RtSolverRoutePlanner.supplier(opt2))
                  .setCommunicator(RtSolverBidder.supplier(objFunc, cih))
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
            // Central.solverConfiguration(
            Opt2.builder()
                .withObjectiveFunction(objFunc)
                .buildRealtimeSolverSupplier(),
            ""))
            // , true))
            .addModel(RealtimeClockLogger.builder())
            .build())

        .showGui(View.builder()
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
    INSTANCE {
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
    }
  }

  static void appendSimResult(SimulationResult sr, File destFile) {
    final String pc = sr.getSimArgs().getScenario().getProblemClass().getId();
    final String id = sr.getSimArgs().getScenario().getProblemInstanceId();
    final int numVehicles = FluentIterable
        .from(sr.getSimArgs().getScenario().getEvents())
        .filter(AddVehicleEvent.class).size();
    final int numParcels = FluentIterable
        .from(sr.getSimArgs().getScenario().getEvents())
        .filter(AddParcelEvent.class).size();
    final Gendreau06ObjectiveFunction objFunc =
      (Gendreau06ObjectiveFunction) sr.getSimArgs().getObjectiveFunction();

    try {
      if (sr.getSimArgs().getScenario()
          .getProblemClass() instanceof GendreauProblemClass) {
        final String scenarioName = Joiner.on("-").join(pc, id);
        if (sr.getResultObject() instanceof FailureStrategy) {
          final String line = Joiner.on(",")
              .appendTo(new StringBuilder(),
                asList(-1, -1, -1, -1, -1, -1, -1, false,
                  scenarioName, sr.getSimArgs().getRandomSeed(), -1,
                  numVehicles, numParcels))
              .append(System.lineSeparator())
              .toString();
          Files.append(line, destFile, Charsets.UTF_8);
        } else {

          final ExperimentInfo ei = (ExperimentInfo) sr.getResultObject();
          final StatisticsDTO stats = ei.getStats();

          final double cost = objFunc.computeCost(stats);
          final double travelTime = objFunc.travelTime(stats);
          final double tardiness = objFunc.tardiness(stats);
          final double overTime = objFunc.overTime(stats);
          final boolean isValidResult = objFunc.isValidResult(stats);
          final long computationTime = stats.computationTime;

          final String line = Joiner.on(",")
              .appendTo(new StringBuilder(),
                asList(-1, -1, -1, cost, travelTime,
                  tardiness, overTime, isValidResult, scenarioName,
                  sr.getSimArgs().getRandomSeed(),
                  computationTime, numVehicles, numParcels))
              .append(System.lineSeparator())
              .toString();

          Files.append(line, destFile, Charsets.UTF_8);
        }
      } else {

        final String scenarioName = Joiner.on("-").join(pc, id);
        final List<String> propsStrings = Files.readLines(new File(
            VANLON_HOLVOET_DATASET + scenarioName + ".properties"),
          Charsets.UTF_8);
        final Map<String, String> properties = Splitter.on("\n")
            .withKeyValueSeparator(" = ")
            .split(Joiner.on("\n").join(propsStrings));

        final double dynamism = Double.parseDouble(properties
            .get("dynamism_bin"));
        final long urgencyMean = Long.parseLong(properties.get("urgency"));
        final double scale = Double.parseDouble(properties.get("scale"));

        final long numOrders =
          Long.parseLong(properties.get("AddParcelEvent"));

        if (sr.getResultObject() instanceof FailureStrategy) {
          final String line = Joiner.on(",")
              .appendTo(new StringBuilder(),
                asList(dynamism, urgencyMean, scale, -1, -1, -1, -1, false,
                  scenarioName, sr.getSimArgs().getRandomSeed(), -1,
                  numVehicles,
                  numOrders))
              .append(System.lineSeparator())
              .toString();
          Files.append(line, destFile, Charsets.UTF_8);
        } else {

          final ExperimentInfo ei = (ExperimentInfo) sr.getResultObject();
          final StatisticsDTO stats = ei.getStats();

          // final StatisticsDTO stats = (StatisticsDTO) sr.getResultObject();
          final double cost = objFunc.computeCost(stats);
          final double travelTime = objFunc.travelTime(stats);
          final double tardiness = objFunc.tardiness(stats);
          final double overTime = objFunc.overTime(stats);
          final boolean isValidResult = objFunc.isValidResult(stats);
          final long computationTime = stats.computationTime;

          final String line = Joiner.on(",")
              .appendTo(new StringBuilder(),
                asList(dynamism, urgencyMean, scale, cost, travelTime,
                  tardiness, overTime, isValidResult, scenarioName,
                  sr.getSimArgs().getRandomSeed(),
                  computationTime, numVehicles, numOrders))
              .append(System.lineSeparator())
              .toString();
          if (!isValidResult) {
            System.err.println("WARNING: FOUND AN INVALID RESULT: ");
            System.err.println(line);
          }
          Files.append(line, destFile, Charsets.UTF_8);
        }
      }
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @AutoValue
  abstract static class ExperimentInfo {
    abstract List<LogEntry> getLog();

    abstract long getRtCount();

    abstract long getStCount();

    abstract StatisticsDTO getStats();

    abstract ImmutableList<MeasuredDeviation> getMeasuredDeviations();

    static ExperimentInfo create(List<LogEntry> log, long rt, long st,
        StatisticsDTO stats, ImmutableList<MeasuredDeviation> dev) {
      return new AutoValue_PerformExperiment_ExperimentInfo(log, rt, st, stats,
          dev);
    }
  }

  static File createExperimentDir(File target) {
    final String timestamp = ISODateTimeFormat.dateHourMinuteSecond()
        .print(System.currentTimeMillis());
    final File experimentDirectory = new File(target, timestamp);
    experimentDirectory.mkdirs();

    final File latest = new File(target, "latest/");
    if (latest.exists()) {
      latest.delete();
    }
    try {
      java.nio.file.Files.createSymbolicLink(
        latest.toPath(),
        experimentDirectory.getAbsoluteFile().toPath());
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
    return experimentDirectory;
  }

  enum LogProcessor implements PostProcessor<ExperimentInfo> {
    INSTANCE {
      @Override
      public ExperimentInfo collectResults(Simulator sim, SimArgs args) {
        final RealtimeClockLogger logger =
          sim.getModelProvider().getModel(RealtimeClockLogger.class);

        // logger.getDeviations()

        final StatisticsDTO stats =
          PostProcessors.statisticsPostProcessor().collectResults(sim, args);

        System.out.println("success: " + args);

        return ExperimentInfo.create(logger.getLog(), logger.getRtCount(),
          logger.getStCount(), stats, logger.getDeviations());
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
