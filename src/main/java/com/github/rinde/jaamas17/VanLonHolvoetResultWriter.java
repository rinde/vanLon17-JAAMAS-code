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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel.AuctionEvent;
import com.github.rinde.jaamas17.SimResult.TimeMeasurements;
import com.github.rinde.logistics.pdptw.mas.comm.Bidder;
import com.github.rinde.logistics.pdptw.mas.route.RoutePlanner;
import com.github.rinde.rinsim.central.SolverTimeMeasurement;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

/**
 *
 * @author Rinde van Lon
 */
public class VanLonHolvoetResultWriter extends ResultWriter {

  public VanLonHolvoetResultWriter(File target,
      Gendreau06ObjectiveFunction objFunc) {
    super(target, objFunc);
  }

  @Override
  public void receive(SimulationResult result) {
    final String configName = result.getSimArgs().getMasConfig().getName();
    final File targetFile = new File(experimentDirectory, configName + ".csv");

    if (!targetFile.exists()) {
      createCSVWithHeader(targetFile);
    }
    appendSimResult(result, targetFile);

    writeTimeLog(result);
    writeBidComputationTimeMeasurements(result);
  }

  void writeBidComputationTimeMeasurements(SimulationResult result) {
    if (!(result.getResultObject() instanceof SimResult)) {
      return;
    }
    final SimResult info = (SimResult) result.getResultObject();
    final TimeMeasurements measurements = info.getTimeMeasurements().get();

    if (measurements.getAuctionEvents().isEmpty()
      || measurements.getBidTimeMeasurements().isEmpty()
      || measurements.getRpTimeMeasurements().isEmpty()) {
      return;
    }
    final SimArgs simArgs = result.getSimArgs();
    final Scenario scenario = simArgs.getScenario();

    final String id = Joiner.on("-").join(
      simArgs.getMasConfig().getName(),
      scenario.getProblemClass().getId(),
      scenario.getProblemInstanceId(),
      simArgs.getRandomSeed(),
      simArgs.getRepetition());

    final File statsDir =
      new File(experimentDirectory, "computation-time-stats");
    statsDir.mkdirs();

    if (!measurements.getAuctionEvents().isEmpty()) {
      final File auctionsFile = new File(statsDir, id + "-auctions.csv");
      final StringBuilder auctionContents = new StringBuilder();
      auctionContents.append("auction_start,auction_end,num_bids")
        .append(System.lineSeparator());
      for (final AuctionEvent e : measurements.getAuctionEvents()) {
        Joiner.on(",").appendTo(auctionContents,
          e.getAuctionStartTime(),
          e.getTime(),
          e.getNumBids());
        auctionContents.append(System.lineSeparator());
      }
      try {
        Files.write(auctionContents, auctionsFile, Charsets.UTF_8);
      } catch (final IOException e) {
        throw new IllegalStateException(e);
      }
    }

    if (!measurements.getBidTimeMeasurements().isEmpty()) {
      final File compFile = new File(statsDir, id + "-bid-computations.csv");
      final ImmutableListMultimap<Bidder<?>, SolverTimeMeasurement> bidMeasurements =
        measurements.getBidTimeMeasurements();
      final StringBuilder compContents = new StringBuilder();
      compContents
        .append("bidder_id,comp_start_sim_time,route_length,duration_ns")
        .append(System.lineSeparator());
      int bidderId = 0;
      for (final Bidder<?> bidder : bidMeasurements.keySet()) {
        final List<SolverTimeMeasurement> ms = bidMeasurements.get(bidder);
        for (final SolverTimeMeasurement m : ms) {
          final int routeLength =
            m.input().getVehicles().get(0).getRoute().get().size();

          Joiner.on(",").appendTo(compContents,
            bidderId,
            m.input().getTime(),
            routeLength,
            m.durationNs());
          compContents.append(System.lineSeparator());
        }
        bidderId++;
      }
      try {
        Files.write(compContents, compFile, Charsets.UTF_8);
      } catch (final IOException e1) {
        throw new IllegalStateException(e1);
      }
    }

    if (!measurements.getRpTimeMeasurements().isEmpty()) {
      final File rpCompFile = new File(statsDir, id + "-rp-computations.csv");
      final ImmutableListMultimap<RoutePlanner, SolverTimeMeasurement> solMeasurements =
        measurements.getRpTimeMeasurements();
      final StringBuilder compContents = new StringBuilder();
      compContents
        .append("route_planner_id,comp_start_sim_time,route_length,duration_ns")
        .append(System.lineSeparator());
      int rpId = 0;
      for (final RoutePlanner rp : solMeasurements.keySet()) {
        final List<SolverTimeMeasurement> ms = solMeasurements.get(rp);
        for (final SolverTimeMeasurement m : ms) {
          final int routeLength =
            m.input().getVehicles().get(0).getRoute().get().size();

          Joiner.on(",").appendTo(compContents,
            rpId,
            m.input().getTime(),
            routeLength,
            m.durationNs());
          compContents.append(System.lineSeparator());
        }
        rpId++;
      }
      try {
        Files.write(compContents, rpCompFile, Charsets.UTF_8);
      } catch (final IOException e1) {
        throw new IllegalStateException(e1);
      }
    }

    // clear measurements to avoid filling memory
    info.getTimeMeasurements().clear();
  }

  @Override
  void appendSimResult(SimulationResult sr, File destFile) {
    final String pc = sr.getSimArgs().getScenario().getProblemClass().getId();
    final String id = sr.getSimArgs().getScenario().getProblemInstanceId();

    try {
      final String scenarioName = Joiner.on("-").join(pc, id);
      final List<String> propsStrings = Files.readLines(new File(
        PerformExperiment.VANLON_HOLVOET_DATASET + scenarioName
          + ".properties"),
        Charsets.UTF_8);
      final Map<String, String> properties = Splitter.on("\n")
        .withKeyValueSeparator(" = ")
        .split(Joiner.on("\n").join(propsStrings));

      final ImmutableMap.Builder<Enum<?>, Object> map =
        ImmutableMap.<Enum<?>, Object>builder()
          .put(OutputFields.SCENARIO_ID, scenarioName)
          .put(OutputFields.DYNAMISM, properties.get("dynamism_bin"))
          .put(OutputFields.URGENCY, properties.get("urgency"))
          .put(OutputFields.SCALE, properties.get("scale"))
          .put(OutputFields.NUM_ORDERS, properties.get("AddParcelEvent"))
          .put(OutputFields.NUM_VEHICLES, properties.get("AddVehicleEvent"))
          .put(OutputFields.RANDOM_SEED, sr.getSimArgs().getRandomSeed())
          .put(OutputFields.REPETITION, sr.getSimArgs().getRepetition());

      addSimOutputs(map, sr, objectiveFunction);

      final String line = MeasureGendreau
        .appendValuesTo(new StringBuilder(), map.build(), getFields())
        .append(System.lineSeparator())
        .toString();
      Files.append(line, destFile, Charsets.UTF_8);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  Iterable<Enum<?>> getFields() {
    return ImmutableList.<Enum<?>>copyOf(OutputFields.values());
  }

}
