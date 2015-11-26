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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Scenario;
import com.github.rinde.rinsim.scenario.measure.Metrics;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Multiset;
import com.google.common.io.Files;

/**
 *
 * @author Rinde van Lon
 */
public class MeasureGendreau {

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {

    final List<Gendreau06Scenario> scns =
      new ArrayList<>(Gendreau06Parser.parser()
          .addDirectory("files/gendreau2006/requests").parse());

    Collections.sort(scns, new Comparator<Gendreau06Scenario>() {
      @Override
      public int compare(Gendreau06Scenario o1, Gendreau06Scenario o2) {
        final int compare =
          o1.getProblemClass().getId().compareTo(o2.getProblemClass().getId());
        if (compare == 0) {
          return o1.getProblemInstanceId().compareTo(o2.getProblemInstanceId());
        }
        return compare;
      }
    });

    final StringBuilder sb = new StringBuilder();
    sb.append(
      "problem_class,instance_id,dynamism,urgency_mean,urgency_sd,num_orders,"
          + "num_vehicles");
    sb.append(System.lineSeparator());

    for (final Gendreau06Scenario scen : scns) {
      final StatisticalSummary urgency = Metrics.measureUrgency(scen);
      final Multiset<Class<?>> counts = Metrics.getEventTypeCounts(scen);

      final long scenarioLength = scen.getProblemClass().duration * 60000;
      final double dyn = Metrics.measureDynamism(scen, scenarioLength);

      Joiner.on(",").appendTo(sb,
        scen.getProblemClass().getId(),
        scen.getProblemInstanceId(),
        dyn,
        urgency.getMean() / 60000d,
        urgency.getStandardDeviation() / 60000d,
        counts.count(AddParcelEvent.class),
        counts.count(AddVehicleEvent.class));
      sb.append(System.lineSeparator());
    }

    final File targetFile = new File("files/gendreau.csv");

    Files.write(sb.toString(), targetFile, Charsets.UTF_8);
    System.out.println("Results written to " + targetFile.getAbsolutePath());
  }

}
