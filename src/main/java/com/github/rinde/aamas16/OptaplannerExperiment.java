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

import com.github.rinde.logistics.pdptw.solver.optaplanner.OptaplannerSolver;
import com.github.rinde.rinsim.central.Central;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Parser;

/**
 *
 * @author Rinde van Lon
 */
public class OptaplannerExperiment {

  public static void main(String[] args) {
    Experiment.build(Gendreau06ObjectiveFunction.instance())
        // .withThreads(1)
        .addConfiguration(
          Central.solverConfiguration(OptaplannerSolver.validatedSupplier(30d)))
        .addScenarios(Gendreau06Parser.parser()
            .addDirectory("files/gendreau2006/requests")
            .offline()
            .parse())
        .perform();
  }
}
