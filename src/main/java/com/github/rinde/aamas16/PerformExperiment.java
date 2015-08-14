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

import java.nio.file.Paths;

import com.github.rinde.logistics.pdptw.solver.CheapestInsertionHeuristic;
import com.github.rinde.logistics.pdptw.solver.Opt2;
import com.github.rinde.rinsim.central.Central;
import com.github.rinde.rinsim.experiment.CommandLineProgress;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.PostProcessors;
import com.github.rinde.rinsim.io.FileProvider;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.google.common.base.Optional;

/**
 *
 * @author Rinde van Lon
 */
public class PerformExperiment {
  static final Gendreau06ObjectiveFunction SUM = Gendreau06ObjectiveFunction
      .instance();

  static final String DATASET = "files/dataset/";
  static final String RESULTS = "files/results/";

  public static void main(String[] args) {
    final long time = System.currentTimeMillis();
    final Experiment.Builder experimentBuilder = Experiment
        .build(SUM)
        .computeLocal()
        .withRandomSeed(123)
        .withThreads(4)
        .repeat(1)
        .addScenarios(FileProvider.builder()
            .add(Paths.get(DATASET))
            .filter("glob:**[0].scen"))
        .addResultListener(new CommandLineProgress(System.out))
        .usePostProcessor(PostProcessors.statisticsPostProcessor())
        .addConfiguration(Central.solverConfiguration(
          Opt2.breadthFirstSupplier(CheapestInsertionHeuristic.supplier(SUM),
            SUM),
          "CheapInsert"));

    final Optional<ExperimentResults> results =
      experimentBuilder.perform(System.out, args);
    final long duration = System.currentTimeMillis() - time;
    if (!results.isPresent()) {
      return;
    }

    System.out.println("Done, computed " + results.get().getResults().size()
        + " simulations in " + duration / 1000d + "s");

  }
}
