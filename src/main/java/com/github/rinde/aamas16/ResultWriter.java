/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import com.github.rinde.aamas16.PerformExperiment.ExperimentInfo;
import com.github.rinde.rinsim.core.model.time.MeasuredDeviation;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.experiment.PostProcessor.FailureStrategy;
import com.github.rinde.rinsim.experiment.ResultListener;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

abstract class ResultWriter implements ResultListener {
  final File experimentDirectory;
  final File timeDeviationsDirectory;

  public ResultWriter(File target) {
    experimentDirectory = target;
    timeDeviationsDirectory = new File(target, "time-deviations");
    timeDeviationsDirectory.mkdirs();
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
  public void doneComputing(ExperimentResults results) {
    final Multimap<MASConfiguration, SimulationResult> groupedResults =
      LinkedHashMultimap.create();
    for (final SimulationResult sr : results.sortedResults()) {
      groupedResults.put(sr.getSimArgs().getMasConfig(), sr);
    }

    for (final MASConfiguration config : groupedResults.keySet()) {
      final Collection<SimulationResult> group = groupedResults.get(config);

      final File configResult =
        new File(experimentDirectory, config.getName() + "-final.csv");

      // deletes the file in case it already exists
      configResult.delete();
      createCSVWithHeader(configResult);
      for (final SimulationResult sr : group) {
        appendSimResult(sr, configResult);
      }
    }

  }

  abstract Iterable<Enum<?>> getFields();

  abstract void appendSimResult(SimulationResult sr, File destFile);

  void createCSVWithHeader(File f) {
    try {
      Files.createParentDirs(f);
      Files.append(
        Joiner.on(",").appendTo(new StringBuilder(), getFields())
            .append(System.lineSeparator()),
        f,
        Charsets.UTF_8);
    } catch (final IOException e1) {
      throw new IllegalStateException(e1);
    }
  }

  static void appendTimeLogSummary(SimulationResult sr, File target) {

    if (sr.getResultObject() instanceof PerformExperiment.ExperimentInfo) {

      final PerformExperiment.ExperimentInfo info =
        (PerformExperiment.ExperimentInfo) sr.getResultObject();

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

  static void addSimOutputs(ImmutableMap.Builder<Enum<?>, Object> map,
      SimulationResult sr) {
    if (sr.getResultObject() instanceof FailureStrategy) {
      map.put(OutputFields.COST, -1)
          .put(OutputFields.TRAVEL_TIME, -1)
          .put(OutputFields.TARDINESS, -1)
          .put(OutputFields.OVER_TIME, -1)
          .put(OutputFields.IS_VALID, false)
          .put(OutputFields.COMP_TIME, -1);
    } else {
      final ExperimentInfo ei = (ExperimentInfo) sr.getResultObject();
      final StatisticsDTO stats = ei.getStats();
      final Gendreau06ObjectiveFunction objFunc =
        (Gendreau06ObjectiveFunction) sr.getSimArgs().getObjectiveFunction();
      map.put(OutputFields.COST, objFunc.computeCost(stats))
          .put(OutputFields.TRAVEL_TIME, objFunc.travelTime(stats))
          .put(OutputFields.TARDINESS, objFunc.tardiness(stats))
          .put(OutputFields.OVER_TIME, objFunc.overTime(stats))
          .put(OutputFields.IS_VALID, objFunc.isValidResult(stats))
          .put(OutputFields.COMP_TIME, stats.computationTime);
      if (!objFunc.isValidResult(stats)) {
        System.err.println("WARNING: FOUND AN INVALID RESULT: ");
        System.err.println(map.build());
      }
    }
  }

  void writeTimeLog(SimulationResult result) {
    final String configName = result.getSimArgs().getMasConfig().getName();
    final File timeLogSummaryFile =
      new File(experimentDirectory, configName + "-timelog-summary.csv");

    if (!timeLogSummaryFile.exists()) {
      createTimeLogSummaryHeader(timeLogSummaryFile);
    }
    appendTimeLogSummary(result, timeLogSummaryFile);
    createTimeLog(result, timeDeviationsDirectory);
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

  enum OutputFields {
    DYNAMISM,

    URGENCY,

    SCALE,

    COST,

    TRAVEL_TIME,

    TARDINESS,

    OVER_TIME,

    IS_VALID,

    SCENARIO_ID,

    RANDOM_SEED,

    COMP_TIME,

    NUM_VEHICLES,

    NUM_ORDERS;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

}
