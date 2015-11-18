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

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import org.joda.time.format.ISODateTimeFormat;

import com.github.rinde.logistics.pdptw.mas.TruckFactory.DefaultTruckFactory;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionPanel;
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
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.experiment.PostProcessor;
import com.github.rinde.rinsim.experiment.PostProcessor.FailureStrategy;
import com.github.rinde.rinsim.experiment.PostProcessors;
import com.github.rinde.rinsim.experiment.ResultListener;
import com.github.rinde.rinsim.io.FileProvider;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.pdptw.common.RouteRenderer;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.TimeLinePanel;
import com.github.rinde.rinsim.scenario.Scenario;
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

import net.openhft.affinity.AffinityLock;

/**
 *
 * @author Rinde van Lon
 */
public class PerformExperiment {
  static final Gendreau06ObjectiveFunction SUM =
    Gendreau06ObjectiveFunction.instance();

  static final String DATASET = "files/dataset/";
  static final String RESULTS = "files/results/";

  public static void main(String[] args) throws IOException {
    final StochasticSupplier<RealtimeSolver> cih =
      SolverToRealtimeAdapter.create(CheapestInsertionHeuristic.supplier(SUM));

    final StochasticSupplier<RealtimeSolver> opt2 =
      SolverToRealtimeAdapter.create(
        Opt2.breadthFirstSupplier(
          CheapestInsertionHeuristic.supplier(SUM), SUM));

    // Scenario s =
    // ScenarioIO.read(Paths.get(DATASET, "0.80-5-10.00-0.scen"));
    //
    // s = Scenario.builder(s)
    // .removeModelsOfType(TimeModel.AbstractBuilder.class)
    // .addModel(TimeModel.builder()
    // .withRealTime()
    // .withTickLength(250))
    // .build();

    // final List<TimedEvent> events = Scenario.builder(s)
    // .filterEvents(instanceOf(AddParcelEvent.class)).build()
    // .getEvents().subList(0, 10);
    // s = Scenario.builder(s)
    // // .ensureFrequency(instanceOf(AddVehicleEvent.class), 2)
    // .filterEvents(not(instanceOf(AddParcelEvent.class)))
    // .addEvents(events)
    // .build();
    final File experimentDir = createExperimentDir(new File(RESULTS));

    final long time = System.currentTimeMillis();
    final Experiment.Builder experimentBuilder = Experiment
        .build(SUM)
        .computeLocal()
        .withRandomSeed(123)
        .withThreads(1)
        .repeat(3)
        .addResultListener(new IncrementalResultWriter(experimentDir))
        .addResultListener(new CommandLineProgress(System.out))
        // GENDREAU SCENARIOS
        .addScenarios(FileProvider.builder()
            .add(Paths.get("files/gendreau2006/requests"))
            .filter("glob:**req_rapide_1_240_24"))
        .setScenarioReader(Functions.compose(ScenarioConverter.INSTANCE,
          Gendreau06Parser.reader()))
        // DYN-URG-SCL SCENARIOS
        // .addScenarios(FileProvider.builder()
        // .add(Paths.get(DATASET))
        // .filter("glob:**-[0-9].scen"))
        // .setScenarioReader(ScenarioIO.readerAdapter(ScenarioConverter.INSTANCE))
        .usePostProcessor(LogProcessor.INSTANCE)
        .addConfiguration(MASConfiguration.pdptwBuilder()
            .setName("ReAuction-2optRP-cihBID")
            .addEventHandler(AddVehicleEvent.class,
              DefaultTruckFactory.builder()
                  .setRoutePlanner(RtSolverRoutePlanner.supplier(opt2))
                  .setCommunicator(RtSolverBidder.supplier(SUM, cih))
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

        // .addConfiguration(
        // MASConfiguration.builder(
        // RtCentral.solverConfigurationAdapt(
        // // Opt2.breadthFirstSupplier(
        // SolverValidator.wrap(
        // RandomSolver.supplier()
        // // CheapestInsertionHeuristic.supplier(SUM)),
        // // SUM),
        // // "CheapInsert"))
        // ), "random"))
        // .addModel(RealtimeClockLogger.builder())
        // .build())

        // random solver
        // .addConfiguration(MASConfiguration.builder(
        // RtCentral.solverConfigurationAdapt(
        // SolverValidator.wrap(RandomSolver.supplier()), "random"))
        // .addModel(RealtimeClockLogger.builder())
        // .addEventHandler(AddParcelEvent.class, new DebugParcelCreator())
        // .build())

        // cheapest insertion
        .addConfiguration(MASConfiguration.builder(
          RtCentral.solverConfigurationAdapt(
            CheapestInsertionHeuristic.supplier(SUM), "", true))
            .addModel(RealtimeClockLogger.builder())
            .build())

        // 2-opt cheapest insertion
        .addConfiguration(MASConfiguration.builder(
          RtCentral.solverConfigurationAdapt(
            Opt2.breadthFirstSupplier(
              CheapestInsertionHeuristic.supplier(SUM), SUM),
            "", true))
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
            .with(AuctionPanel.builder())
            .with(TimeLinePanel.builder())
            .with(RtSolverPanel.builder())
            .withResolution(1280, 1024));

    final Optional<ExperimentResults> results =
      experimentBuilder.perform(System.out, args);
    final long duration = System.currentTimeMillis() - time;
    if (!results.isPresent()) {
      return;
    }

    System.out.println("Done, computed " + results.get().getResults().size()
        + " simulations in " + duration / 1000d + "s");

    final Multimap<MASConfiguration, SimulationResult> groupedResults =
      LinkedHashMultimap.create();
    for (final SimulationResult sr : results.get().sortedResults()) {
      groupedResults.put(sr.getSimArgs().getMasConfig(), sr);
    }

    for (final MASConfiguration config : groupedResults.keySet()) {
      final Collection<SimulationResult> group = groupedResults.get(config);

      final File configResult =
        new File(experimentDir, config.getName() + "-final.csv");

      // deletes the file in case it already exists
      configResult.delete();
      createCSVWithHeader(configResult);
      for (final SimulationResult sr : group) {
        appendSimResult(sr, configResult);
      }
    }

  }

  static void createTimeLog(SimulationResult sr, File experimentDir) {
    if (!(sr.getResultObject() instanceof ExperimentInfo)) {
      return;
    }
    final SimArgs simArgs = sr.getSimArgs();
    final Scenario scenario = simArgs.getScenario();

    final String id = Joiner.on("-").join(
      simArgs.getMasConfig().getName(),
      scenario.getProblemClass().getId(),
      scenario.getProblemInstanceId(),
      simArgs.getRandomSeed());

    final File deviationsFile = new File(experimentDir, id + "-deviations.csv");
    final File iatFile = new File(experimentDir, id + "-interarrivaltimes.csv");

    final ExperimentInfo info = (ExperimentInfo) sr.getResultObject();

    try (FileWriter writer = new FileWriter(deviationsFile)) {
      deviationsFile.createNewFile();
      for (final MeasuredDeviation md : info.getMeasuredDeviations()) {
        writer.write(Long.toString(md.getDeviationNs()));
        writer.write(System.lineSeparator());
      }
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
    try (FileWriter writer = new FileWriter(iatFile)) {
      iatFile.createNewFile();
      for (final MeasuredDeviation md : info.getMeasuredDeviations()) {
        writer.write(Long.toString(md.getInterArrivalTime()));
        writer.write(System.lineSeparator());
      }
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  static void createTimeLogSummaryHeader(File target) {
    try {
      Files.append(Joiner.on(',').join(
        "problem-class",
        "instance",
        "config",
        "random-seed",
        "measured-deviations",
        "sum-deviations",
        "avg-deviation",
        "sum-correction",
        "avg-correction",
        "avg-interarrival-time",
        "rt-count",
        "st-count\n"), target, Charsets.UTF_8);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  static void appendTimeLogSummary(SimulationResult sr, File target) {

    if (sr.getResultObject() instanceof ExperimentInfo) {

      final ExperimentInfo info = (ExperimentInfo) sr.getResultObject();

      final int totalMeasuredDeviations = info.getMeasuredDeviations().size();
      long sumDeviationNs = 0;
      long sumCorrectionNs = 0;
      long sumIatNs = 0;
      for (final MeasuredDeviation md : info.getMeasuredDeviations()) {
        sumDeviationNs += md.getDeviationNs();
        sumCorrectionNs += md.getCorrectionNs();
        sumIatNs += md.getInterArrivalTime();
      }

      try {
        Files.append(Joiner.on(',').join(
          sr.getSimArgs().getScenario().getProblemClass().getId(),
          sr.getSimArgs().getScenario().getProblemInstanceId(),
          sr.getSimArgs().getMasConfig().getName(),
          sr.getSimArgs().getRandomSeed(),
          totalMeasuredDeviations,
          sumDeviationNs,
          totalMeasuredDeviations == 0 ? 0
              : sumDeviationNs / totalMeasuredDeviations,
          sumCorrectionNs,
          totalMeasuredDeviations == 0 ? 0
              : sumCorrectionNs / totalMeasuredDeviations,
          sumIatNs / totalMeasuredDeviations,
          info.getRtCount(),
          info.getStCount() + "\n"), target, Charsets.UTF_8);
      } catch (final IOException e) {
        throw new IllegalStateException(e);
      }

    }
  }

  enum ScenarioConverter implements Function<Scenario, Scenario> {
    INSTANCE {
      @Override
      public Scenario apply(@Nullable Scenario input) {
        final Scenario s = verifyNotNull(input);
        return Scenario.builder(s)
            .removeModelsOfType(TimeModel.AbstractBuilder.class)
            .addModel(TimeModel.builder().withRealTime().withTickLength(250))
            .setStopCondition(StopConditions.or(s.getStopCondition(),
              StopConditions.limitedTime(10 * 60 * 60 * 1000)))
            .build();
      }
    }
  }

  static void createCSVWithHeader(File f) {
    try {
      Files.createParentDirs(f);

      Files.append(
        "dynamism,urgency,scale,cost,travel_time,tardiness,over_time,is_valid,"
            + "scenario_id,random_seed,comp_time,num_vehicles,num_orders\n",
        f,
        Charsets.UTF_8);
    } catch (final IOException e1) {
      throw new IllegalStateException(e1);
    }
  }

  static void appendSimResult(SimulationResult sr, File destFile) {
    final String pc = sr.getSimArgs().getScenario().getProblemClass()
        .getId();
    final String id = sr.getSimArgs().getScenario().getProblemInstanceId();
    final int numVehicles = FluentIterable
        .from(sr.getSimArgs().getScenario().getEvents())
        .filter(AddVehicleEvent.class).size();
    final int numParcels = FluentIterable
        .from(sr.getSimArgs().getScenario().getEvents())
        .filter(AddParcelEvent.class).size();

    try {
      if (sr.getSimArgs().getScenario()
          .getProblemClass() instanceof GendreauProblemClass) {
        final String scenarioName = Joiner.on("-").join(pc, id);
        if (sr.getResultObject() instanceof FailureStrategy) {
          final String line = Joiner.on(",")
              .appendTo(new StringBuilder(),
                asList(-1, -1, -1, -1, false,
                  scenarioName, sr.getSimArgs().getRandomSeed(), -1,
                  numVehicles, numParcels))
              .append(System.lineSeparator())
              .toString();
          Files.append(line, destFile, Charsets.UTF_8);
        } else {

          final ExperimentInfo ei = (ExperimentInfo) sr.getResultObject();
          final StatisticsDTO stats = ei.getStats();

          final double cost = SUM.computeCost(stats);
          final double travelTime = SUM.travelTime(stats);
          final double tardiness = SUM.tardiness(stats);
          final double overTime = SUM.overTime(stats);
          final boolean isValidResult = SUM.isValidResult(stats);
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
            DATASET + scenarioName + ".properties"),
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
          final double cost = SUM.computeCost(stats);
          final double travelTime = SUM.travelTime(stats);
          final double tardiness = SUM.tardiness(stats);
          final double overTime = SUM.overTime(stats);
          final boolean isValidResult = SUM.isValidResult(stats);
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

  static class IncrementalResultWriter implements ResultListener {
    final File experimentDirectory;

    public IncrementalResultWriter(File target) {
      experimentDirectory = target;
    }

    @Override
    public void startComputing(int numberOfSimulations,
        ImmutableSet<MASConfiguration> configurations,
        ImmutableSet<Scenario> scenarios,
        int repetitions) {

      final StringBuilder sb = new StringBuilder("Experiment summary");
      sb.append(System.lineSeparator())
          .append("Number of simulations: ")
          .append(numberOfSimulations)
          .append(System.lineSeparator())
          .append("Number of configurations: ")
          .append(configurations.size())
          .append(System.lineSeparator())
          .append("Number of scenarios: ")
          .append(scenarios.size())
          .append(System.lineSeparator())
          .append("Number of repetitions: ")
          .append(repetitions)
          .append(System.lineSeparator())
          .append("Configurations:")
          .append(System.lineSeparator());

      for (final MASConfiguration config : configurations) {
        sb.append(config.getName())
            .append(System.lineSeparator());
      }

      final File setup = new File(experimentDirectory, "experiment-setup.txt");
      try {
        setup.createNewFile();
        Files.write(sb.toString(), setup, Charsets.UTF_8);
      } catch (final IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public void receive(SimulationResult result) {
      final String configName = result.getSimArgs().getMasConfig().getName();
      final File targetFile =
        new File(experimentDirectory, configName + ".csv");

      if (!targetFile.exists()) {
        createCSVWithHeader(targetFile);
      }

      appendSimResult(result, targetFile);

      final File timeLogResult =
        new File(experimentDirectory, configName + "-timelog-summary.csv");

      if (!timeLogResult.exists()) {
        createTimeLogSummaryHeader(timeLogResult);
      }
      appendTimeLogSummary(result, timeLogResult);
      createTimeLog(result, experimentDirectory);
    }

    @Override
    public void doneComputing() {}

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
