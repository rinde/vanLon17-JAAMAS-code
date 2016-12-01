/*
 * Copyright (C) 2015-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.jaamas17;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verifyNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.logistics.pdptw.mas.TruckFactory.DefaultTruckFactory;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionPanel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionStopConditions;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionTimeStatsLogger;
import com.github.rinde.logistics.pdptw.mas.comm.DoubleBid;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder.BidFunction;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder.BidFunctions;
import com.github.rinde.logistics.pdptw.mas.route.RoutePlannerStatsLogger;
import com.github.rinde.logistics.pdptw.mas.route.RtSolverRoutePlanner;
import com.github.rinde.logistics.pdptw.solver.CheapestInsertionHeuristic;
import com.github.rinde.logistics.pdptw.solver.Opt2;
import com.github.rinde.logistics.pdptw.solver.optaplanner.OptaplannerSolvers;
import com.github.rinde.rinsim.central.rt.RtCentral;
import com.github.rinde.rinsim.central.rt.RtSolverModel;
import com.github.rinde.rinsim.central.rt.RtSolverPanel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.experiment.CommandLineProgress;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.Experiment.Builder;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.io.FileProvider;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.pdptw.common.RoutePanel;
import com.github.rinde.rinsim.pdptw.common.RouteRenderer;
import com.github.rinde.rinsim.pdptw.common.TimeLinePanel;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioConverters;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * This is the main class for all experiments.
 * @author Rinde van Lon
 */
public final class PerformExperiment {
  static final Logger LOGGER = LoggerFactory.getLogger(PerformExperiment.class);

  static final String VANLON_HOLVOET_DATASET = "files/vanLonHolvoet15/";
  static final String GENDREAU_DATASET = "files/gendreau2006/requests";
  static final String RESULTS_MAIN_DIR = "files/results/";

  private static final long CENTRAL_UNIMPROVED_MS = 10000L;

  private PerformExperiment() {}

  enum ExperimentType {
    GENDREAU(Gendreau06ObjectiveFunction.instance()) {
      @Override
      void apply(Builder bldr) {
        bldr
          .addScenarios(FileProvider.builder().add(Paths.get(GENDREAU_DATASET))
            .filter("glob:**req_rapide_**"))
          .setScenarioReader(
            Functions.compose(ScenarioConverter.TO_ONLINE_REALTIME_250,
              Gendreau06Parser.reader()))
          .addResultListener(
            new GendreauResultWriter(experimentDir, getObjectiveFunction()));
      }
    },

    GENDREAU_SIMULATED(Gendreau06ObjectiveFunction.instance()) {
      @Override
      void apply(Builder b) {
        b.addScenarios(FileProvider.builder().add(Paths.get(GENDREAU_DATASET))
          .filter("glob:**req_rapide_**"))
          .setScenarioReader(
            Functions.compose(ScenarioConverter.TO_ONLINE_SIMULATED_250,
              Gendreau06Parser.reader()))
          .addResultListener(
            new GendreauResultWriter(experimentDir, getObjectiveFunction()));
      }
    },

    GENDREAU_OFFLINE(Gendreau06ObjectiveFunction.instance()) {
      @Override
      void apply(Builder bldr) {
        bldr
          .addScenarios(FileProvider.builder().add(Paths.get(GENDREAU_DATASET))
            .filter("glob:**req_rapide_**"))
          .setScenarioReader(Functions.compose(ScenarioConverter.TO_OFFLINE,
            Gendreau06Parser.reader()))
          .addResultListener(
            new GendreauResultWriter(experimentDir, getObjectiveFunction()));
      }
    },

    /**
     * Experiment using the Van Lon & Holvoet (2015) dataset.
     */
    VAN_LON15(Gendreau06ObjectiveFunction.instance(50d)) {
      @Override
      void apply(Builder bldr) {
        bldr
          .addScenarios(FileProvider.builder()
            .add(Paths.get(VANLON_HOLVOET_DATASET)).filter("glob:**[0-9].scen"))
          .setScenarioReader(
            ScenarioIO.readerAdapter(ScenarioConverter.TO_ONLINE_REALTIME_250))
          .addResultListener(new VanLonHolvoetResultWriter(experimentDir,
            getObjectiveFunction()));
      }
    },

    VAN_LON15_OFFLINE(Gendreau06ObjectiveFunction.instance(50d)) {
      @Override
      void apply(Builder bldr) {
        bldr
          .addScenarios(FileProvider.builder()
            .add(Paths.get(VANLON_HOLVOET_DATASET)).filter("glob:**[0-9].scen"))
          .setScenarioReader(
            ScenarioIO.readerAdapter(ScenarioConverter.TO_OFFLINE))
          .addResultListener(new VanLonHolvoetResultWriter(experimentDir,
            getObjectiveFunction()));
      }
    },

    VAN_LON15_SIMULATED(Gendreau06ObjectiveFunction.instance(50d)) {
      @Override
      void apply(Builder bldr) {
        bldr
          .addScenarios(FileProvider.builder()
            .add(Paths.get(VANLON_HOLVOET_DATASET)).filter("glob:**[0-9].scen"))
          .setScenarioReader(
            ScenarioIO.readerAdapter(ScenarioConverter.TO_ONLINE_SIMULATED_250))
          .addResultListener(new VanLonHolvoetResultWriter(experimentDir,
            getObjectiveFunction()));
      }
    },

    /**
     * Investigate one setting of the Van Lon & Holvoet (2015) dataset with many
     * repetitions.
     */
    TIME_DEVIATION(Gendreau06ObjectiveFunction.instance(50d)) {
      @Override
      void apply(Builder bldr) {
        bldr
          .addScenarios(
            FileProvider.builder().add(Paths.get(VANLON_HOLVOET_DATASET))
              .filter("glob:**0.50-20-10.00-[0-9].scen"))
          .setScenarioReader(
            ScenarioIO.readerAdapter(ScenarioConverter.TO_ONLINE_REALTIME_250))
          .repeatSeed(10).addResultListener(new VanLonHolvoetResultWriter(
            experimentDir, getObjectiveFunction()));
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

  enum Configurations {

    MAS_TUNING_B_MS, MAS_TUNING_RP_AND_B_MS, MAS_TUNING_3_REAUCT, RT_CIH_OPT2_SOLVERS, MAIN_CONFIGS, OPTAPLANNER_TUNING, OPTAPLANNER_SENSITIVITY;

    static ImmutableList<Configurations> parse(String string) {
      final ImmutableList.Builder<Configurations> listBuilder =
        ImmutableList.builder();
      for (final String part : string.split(",")) {
        listBuilder.add(valueOf(part));
      }
      return listBuilder.build();
    }
  }

  public static void main(String[] args) throws IOException {
    System.out.println(System.getProperty("java.vm.name") + ", "
      + System.getProperty("java.vm.vendor") + ", "
      + System.getProperty("java.vm.version") + " (runtime version: "
      + System.getProperty("java.runtime.version") + ")");
    System.out.println(System.getProperty("os.name") + " "
      + System.getProperty("os.version") + " "
      + System.getProperty("os.arch"));
    checkArgument(System.getProperty("java.vm.name").contains("Server"),
      "Experiments should be run in a JVM in server mode.");
    checkArgument(args.length > 2 && args[0].equals("-exp"),
      "The type of experiment that should be run must be specified as follows: "
        + "\'-exp vanlon15|gendreau|timedeviation\', this option must be the "
        + "first in the list.");

    final ExperimentType experimentType = ExperimentType.find(args[1]);
    System.out.println(experimentType);

    final List<Configurations> configs = Configurations.parse(args[2]);
    System.out.println(configs);

    final String[] expArgs = new String[args.length - 3];
    System.arraycopy(args, 3, expArgs, 0, args.length - 3);

    final Gendreau06ObjectiveFunction objFunc =
      experimentType.getObjectiveFunction();

    final OptaplannerSolvers.Builder opFfdFactory =
      OptaplannerSolvers.builder().withSolverFromBenchmark(
        "com/github/rinde/jaamas17/firstFitDecreasingBenchmark.xml")
        .withObjectiveFunction(objFunc);
    final OptaplannerSolvers.Builder opCiFactory =
      OptaplannerSolvers.builder().withSolverFromBenchmark(
        "com/github/rinde/jaamas17/cheapestInsertionBenchmark.xml")
        .withObjectiveFunction(objFunc);

    final long time = System.currentTimeMillis();
    final Experiment.Builder experimentBuilder = Experiment.builder()
      .computeLocal()
      .withRandomSeed(123)
      .withThreads((int) Math
        .floor((Runtime.getRuntime().availableProcessors() - 1) / 2d))
      .repeat(1)
      .withWarmup(30000)
      .addResultListener(new CommandLineProgress(System.out))
      .usePostProcessor(new JaamasPostProcessor(objFunc));

    experimentType.apply(experimentBuilder);

    for (final Configurations config : configs) {
      switch (config) {

      case OPTAPLANNER_TUNING:
        experimentBuilder.addConfigurations(
          optaplannerTuningConfigs(opFfdFactory, opCiFactory, objFunc));
        break;

      case OPTAPLANNER_SENSITIVITY:
        experimentBuilder.addConfigurations(
          optaplannerSensitivityConfigs(opFfdFactory, opCiFactory, objFunc));
        break;

      case MAS_TUNING_B_MS:
        experimentBuilder
          .addConfigurations(masTuning1BmsConfigs(opFfdFactory, objFunc));
        break;

      case MAS_TUNING_RP_AND_B_MS:
        experimentBuilder
          .addConfigurations(masTuning2RPandBmsConfigs(opFfdFactory, objFunc));
        break;

      case MAS_TUNING_3_REAUCT:
        experimentBuilder
          .addConfigurations(masTuning3ReauctConfigs(opFfdFactory, objFunc));
        break;

      case RT_CIH_OPT2_SOLVERS:
        experimentBuilder.addConfigurations(rtCihOpt2Solvers(objFunc));
        break;

      case MAIN_CONFIGS:
        experimentBuilder.addConfigurations(mainConfigs(opFfdFactory, objFunc));
        break;

      }
    }

    experimentBuilder
      .showGui(View.builder()
        .withAutoPlay()
        .withAutoClose()
        .withSpeedUp(128)
        // .withFullScreen()
        .withTitleAppendix("JAAMAS 2017 Experiment")
        .with(RoadUserRenderer.builder().withToStringLabel())
        .with(RouteRenderer.builder())
        .with(PDPModelRenderer.builder())
        .with(PlaneRoadModelRenderer.builder())
        .with(AuctionPanel.builder())
        .with(RoutePanel.builder())
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

  static List<MASConfiguration> mainConfigs(
      OptaplannerSolvers.Builder opFfdFactory, ObjectiveFunction objFunc) {
    final long rpMs = 100;
    final long bMs = 20;
    final long maxAuctionDurationSoft = 10000L;

    final List<MASConfiguration> configs = new ArrayList<>();
    configs.add(createMAS(opFfdFactory, objFunc, rpMs, bMs,
      maxAuctionDurationSoft, false, 0L, false));
    final String solverKey =
      "Step-counting-hill-climbing-with-entity-tabu-and-strategic-oscillation";

    final long centralUnimprovedMs = 10000L;
    configs.add(createCentral(
      opFfdFactory.withSolverKey(solverKey)
        .withUnimprovedMsLimit(centralUnimprovedMs),
      "OP.RT-FFD-" + solverKey));
    return configs;
  }

  static List<MASConfiguration> optaplannerTuningConfigs(
      OptaplannerSolvers.Builder opFfdFactory,
      OptaplannerSolvers.Builder opCiFactory, ObjectiveFunction objFunc) {

    final List<MASConfiguration> configs = new ArrayList<>();

    final OptaplannerSolvers.Builder opBuilder = OptaplannerSolvers.builder()
      .withObjectiveFunction(objFunc);

    configs.add(createCentral(opBuilder.withFirstFitDecreasingSolver(),
      "OP.RT-FFD"));
    for (final String solverKey : opFfdFactory.getSupportedSolverKeys()) {
      configs.add(createCentral(opFfdFactory.withSolverKey(solverKey)
        .withUnimprovedMsLimit(CENTRAL_UNIMPROVED_MS),
        "OP.RT-FFD-" + solverKey));
    }
    configs.add(createCentral(opBuilder.withCheapestInsertionSolver(),
      "OP.RT-CI"));
    for (final String solverKey : opCiFactory.getSupportedSolverKeys()) {
      configs.add(createCentral(opCiFactory.withSolverKey(solverKey)
        .withUnimprovedMsLimit(CENTRAL_UNIMPROVED_MS),
        "OP.RT-CI-" + solverKey));
    }
    return configs;
  }

  static List<MASConfiguration> optaplannerSensitivityConfigs(
      OptaplannerSolvers.Builder opFfdFactory,
      OptaplannerSolvers.Builder opCiFactory, ObjectiveFunction objFunc) {
    final OptaplannerSolvers.Builder opBuilder = OptaplannerSolvers.builder()
      .withObjectiveFunction(objFunc);

    System.out.println(opFfdFactory.getSupportedSolverKeys());

    final List<MASConfiguration> configs = new ArrayList<>();
    configs.add(createCentral(opBuilder.withFirstFitDecreasingSolver(),
      "OP.RT-first-fit-decreasing"));
    configs.add(createCentral(opBuilder.withCheapestInsertionSolver(),
      "OP.RT-cheapest-insertion"));
    configs
      .add(createCentral(opFfdFactory.withSolverKey("Tabu-search-entity-tabu")
        .withUnimprovedMsLimit(CENTRAL_UNIMPROVED_MS),
        "OP.RT-first-fit-decreasing-with-tabu"));

    return configs;
  }

  static List<MASConfiguration> masTuning1BmsConfigs(
      OptaplannerSolvers.Builder opFfdFactory, ObjectiveFunction objFunc) {
    final List<MASConfiguration> configs = new ArrayList<>();
    final long rpMs = 2500L;
    final long[] bMsOptions =
      new long[] {1L, 2L, 5L, 8L, 10L, 15L, 20L, 50L, 100L};
    final long maxAuctionDurationSoft = 5000L;

    for (final long bMs : bMsOptions) {
      configs.add(
        createMAS(opFfdFactory, objFunc, rpMs, bMs, maxAuctionDurationSoft,
          true, 0L, true));
    }
    return configs;
  }

  static List<MASConfiguration> masTuning2RPandBmsConfigs(
      OptaplannerSolvers.Builder opFfdFactory, ObjectiveFunction objFunc) {
    final List<MASConfiguration> configs = new ArrayList<>();
    // rp unimproved ms options: 100, 250
    final long[] rpMsOptions = new long[] {100L, 250L};
    // bid unimproved ms options: 8, 15, 20, 25
    final long[] bMsOptions = new long[] {8, 15, 20, 25};
    // max auction duration 10 sec
    final long maxAuctionDurationSoft = 10000L;

    for (final long rpMs : rpMsOptions) {
      for (final long bMs : bMsOptions) {
        configs.add(
          createMAS(opFfdFactory, objFunc, rpMs, bMs, maxAuctionDurationSoft,
            true, 0L, true));
      }
    }
    return configs;
  }

  static List<MASConfiguration> masTuning3ReauctConfigs(
      OptaplannerSolvers.Builder opFfdFactory, ObjectiveFunction objFunc) {
    final List<MASConfiguration> configs = new ArrayList<>();
    final long rpMs = 100;
    final long bMs = 20;
    final long maxAuctionDurationSoft = 10000L;

    final long[] cooldownPeriods =
      new long[] {60 * 1000L, 10 * 60 * 1000L, 20 * 60 * 1000L};

    for (final long cooldownPeriod : cooldownPeriods) {
      configs.add(
        createMAS(opFfdFactory, objFunc, rpMs, bMs, maxAuctionDurationSoft,
          true, cooldownPeriod, true));
    }

    configs.add(
      createMAS(opFfdFactory, objFunc, rpMs, bMs, maxAuctionDurationSoft,
        false, 0L, true));
    return configs;
  }

  static MASConfiguration createMAS(OptaplannerSolvers.Builder opFfdFactory,
      ObjectiveFunction objFunc, long rpMs, long bMs,
      long maxAuctionDurationSoft, boolean enableReauctions,
      long reauctCooldownPeriodMs, boolean computationsLogging) {
    final BidFunction bf = BidFunctions.BALANCED_HIGH;
    final String masSolverName =
      "Step-counting-hill-climbing-with-entity-tabu-and-strategic-oscillation";

    final String suffix;
    if (false == enableReauctions) {
      suffix = "-NO-REAUCT";
    } else if (reauctCooldownPeriodMs > 0) {
      suffix = "-reauctCooldownPeriod-" + reauctCooldownPeriodMs;
    } else {
      suffix = "";
    }

    MASConfiguration.Builder b = MASConfiguration.pdptwBuilder()
      .setName(
        "ReAuction-FFD-" + masSolverName + "-RP-" + rpMs + "-BID-" + bMs + "-"
          + bf + suffix)
      .addEventHandler(AddVehicleEvent.class,
        DefaultTruckFactory.builder()
          .setRoutePlanner(RtSolverRoutePlanner.supplier(
            opFfdFactory.withSolverKey(masSolverName)
              .withUnimprovedMsLimit(rpMs)
              .withTimeMeasurementsEnabled(computationsLogging)
              .buildRealtimeSolverSupplier()))
          .setCommunicator(

            RtSolverBidder.realtimeBuilder(objFunc,
              opFfdFactory.withSolverKey(masSolverName)
                .withUnimprovedMsLimit(bMs)
                .withTimeMeasurementsEnabled(computationsLogging)
                .buildRealtimeSolverSupplier())
              .withBidFunction(bf)
              .withReauctionsEnabled(enableReauctions)
              .withReauctionCooldownPeriod(reauctCooldownPeriodMs))
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
                .<DoubleBid>maxAuctionDuration(maxAuctionDurationSoft))))
        .withMaxAuctionDuration(30 * 60 * 1000L))
      .addModel(RtSolverModel.builder()
        .withThreadPoolSize(3)
        .withThreadGrouping(true))
      .addModel(RealtimeClockLogger.builder());

    if (computationsLogging) {
      b = b.addModel(AuctionTimeStatsLogger.builder())
        .addModel(RoutePlannerStatsLogger.builder());
    }

    return b.build();
  }

  static List<MASConfiguration> rtCihOpt2Solvers(ObjectiveFunction objFunc) {
    return ImmutableList.of(
      // cheapest insertion
      MASConfiguration.builder(RtCentral.solverConfigurationAdapt(
        CheapestInsertionHeuristic.supplier(objFunc), "", true))
        .addModel(RealtimeClockLogger.builder()).build(),
      // 2-opt cheapest insertion
      MASConfiguration.builder(RtCentral.solverConfiguration(Opt2.builder()
        .withObjectiveFunction(objFunc).buildRealtimeSolverSupplier(), ""))
        .addModel(RealtimeClockLogger.builder()).build());
  }

  static void addCentral(Experiment.Builder experimentBuilder,
      OptaplannerSolvers.Builder opBuilder, String name) {
    experimentBuilder.addConfiguration(createCentral(opBuilder, name));
  }

  static MASConfiguration createCentral(OptaplannerSolvers.Builder opBuilder,
      String name) {
    return MASConfiguration.pdptwBuilder()
      .addModel(RtCentral.builder(opBuilder.buildRealtimeSolverSupplier())
        .withContinuousUpdates(true)
        .withThreadGrouping(true))
      .addModel(RealtimeClockLogger.builder())
      .addEventHandler(AddVehicleEvent.class, RtCentral.vehicleHandler())
      .setName(name)
      .build();
  }

  enum ScenarioConverter implements Function<Scenario, Scenario> {
    /**
     * Changes ticksize to 250ms and adds stopcondition with maximum sim time of
     * 10 hours.
     */
    TO_ONLINE_REALTIME_250 {
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
    TO_ONLINE_SIMULATED_250 {
      @Override
      public Scenario apply(@Nullable Scenario input) {
        final Scenario s = verifyNotNull(input);
        return Scenario.builder(s)
          .removeModelsOfType(TimeModel.AbstractBuilder.class)
          .addModel(TimeModel.builder().withTickLength(250))
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
              return AddParcelEvent
                .create(Parcel.builder(((AddParcelEvent) input).getParcelDTO())
                  .orderAnnounceTime(-1).buildDTO());
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
}
