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
package com.github.rinde.jaamas16;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.github.rinde.jaamas16.MeasureGendreau.Property;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

/**
 *
 * @author Rinde van Lon
 */
public class GendreauResultWriter extends ResultWriter {
  private static final double VEHICLES_TO_SCALE = .1;
  private static final String SEPARATOR = "-";

  ImmutableMap<String, ImmutableMap<Property, String>> properties;

  public GendreauResultWriter(File target) {
    super(target);

    try {
      final ImmutableMap.Builder<String, ImmutableMap<Property, String>> mapBuilder =
        ImmutableMap.builder();
      for (final ImmutableMap<Property, String> map : MeasureGendreau
        .read(new File(MeasureGendreau.PROPS_FILE))) {

        final String id =
          map.get(Property.PROBLEM_CLASS)
            + SEPARATOR
            + map.get(Property.INSTANCE_ID);
        mapBuilder.put(id, map);
      }
      properties = mapBuilder.build();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
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
  Iterable<Enum<?>> getFields() {
    return ImmutableList.<Enum<?>>builder()
      .add(OutputFields.values())
      .add(Property.GENDR_ALG)
      .add(Property.GENDR_COST)
      .add(Property.GENDR_TT)
      .add(Property.GENDR_TARD)
      .add(Property.GENDR_OT)
      .build();
  }

  @Override
  void appendSimResult(SimulationResult sr, File destFile) {
    final String pc = sr.getSimArgs().getScenario().getProblemClass().getId();
    final String id = sr.getSimArgs().getScenario().getProblemInstanceId();
    final String scenId = Joiner.on(SEPARATOR).join(pc, id);

    final Map<Property, String> props = properties.get(scenId);
    final int numVehicles = Integer.parseInt(props.get(Property.NUM_VEHICLES));

    try {
      final ImmutableMap.Builder<Enum<?>, Object> map =
        ImmutableMap.<Enum<?>, Object>builder()
          .put(OutputFields.SCENARIO_ID, scenId)
          .put(OutputFields.DYNAMISM, props.get(Property.DYNAMISM))
          .put(OutputFields.URGENCY, props.get(Property.URGENCY_MEAN))
          .put(OutputFields.SCALE, numVehicles * VEHICLES_TO_SCALE)
          .put(OutputFields.NUM_ORDERS, props.get(Property.NUM_ORDERS))
          .put(OutputFields.NUM_VEHICLES, numVehicles)
          .put(OutputFields.RANDOM_SEED, sr.getSimArgs().getRandomSeed())
          .put(OutputFields.REPETITION, sr.getSimArgs().getRepetition())
          .put(Property.GENDR_ALG, props.get(Property.GENDR_ALG))
          .put(Property.GENDR_COST, props.get(Property.GENDR_COST))
          .put(Property.GENDR_TT, props.get(Property.GENDR_TT))
          .put(Property.GENDR_TARD, props.get(Property.GENDR_TARD))
          .put(Property.GENDR_OT, props.get(Property.GENDR_OT));

      addSimOutputs(map, sr);

      final String line = MeasureGendreau
        .appendValuesTo(new StringBuilder(), map.build(), getFields())
        .append(System.lineSeparator())
        .toString();
      Files.append(line, destFile, Charsets.UTF_8);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

}
