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
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

/**
 *
 * @author Rinde van Lon
 */
public class VanLonHolvoetResultWriter extends ResultWriter {

  /**
   * @param target
   */
  public VanLonHolvoetResultWriter(File target) {
    super(target);
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

      final ImmutableMap.Builder<OutputFields, Object> map =
        ImmutableMap.<OutputFields, Object>builder()
            .put(OutputFields.SCENARIO_ID, scenarioName)
            .put(OutputFields.DYNAMISM, properties.get("dynamism_bin"))
            .put(OutputFields.URGENCY, properties.get("urgency"))
            .put(OutputFields.SCALE, properties.get("scale"))
            .put(OutputFields.NUM_ORDERS, properties.get("AddParcelEvent"))
            .put(OutputFields.NUM_VEHICLES, properties.get("AddVehicleEvent"))
            .put(OutputFields.RANDOM_SEED, sr.getSimArgs().getRandomSeed());

      addSimOutputs(map, sr);

      final String line = MeasureGendreau
          .appendValuesTo(new StringBuilder(), map.build(),
            OutputFields.values())
          .append(System.lineSeparator())
          .toString();
      Files.append(line, destFile, Charsets.UTF_8);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

}
