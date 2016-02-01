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

import static com.google.common.base.Preconditions.checkState;

import java.io.File;

import org.optaplanner.core.api.domain.solution.Solution;
import org.optaplanner.persistence.common.api.domain.solution.SolutionFileIO;

import com.github.rinde.logistics.pdptw.solver.optaplanner.OptaplannerSolvers;
import com.github.rinde.logistics.pdptw.solver.optaplanner.PDPSolution;
import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.Solvers;
import com.github.rinde.rinsim.central.Solvers.SimulationConverter;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.central.rt.RtCentral;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.ScenarioController;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Scenario;

/**
 *
 * @author Rinde van Lon
 */
public final class OptaplannerGendreauIO implements SolutionFileIO {

  public OptaplannerGendreauIO() {}

  @Override
  public String getInputFileExtension() {
    return "";
  }

  @Override
  public String getOutputFileExtension() {
    return null;
  }

  @Override
  public PDPSolution read(File inputSolutionFile) {
    final Gendreau06Scenario scenario = Gendreau06Parser.parser()
        .addFile(inputSolutionFile)
        .offline()
        .parse().get(0);

    final ScenarioController.Builder scenContrBuilder =
      ScenarioController.builder(scenario)
          .withEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
          .withEventHandler(AddDepotEvent.class, AddDepotEvent.defaultHandler())
          .withEventHandler(AddParcelEvent.class,
            AddParcelEvent.defaultHandler())
          .withEventHandler(AddVehicleEvent.class, RtCentral.vehicleHandler());

    final Simulator sim = Simulator.builder()
        .addModel(scenContrBuilder)
        .build();

    sim.tick();

    final SimulationConverter conv = Solvers.converterBuilder()
        .with(sim)
        .build();

    final GlobalStateObject gso =
      conv.convert(SolveArgs.create()
          .useAllParcels()
          .useEmptyRoutes(scenario.getProblemClass().vehicles)).state;

    checkState(!gso.getVehicles().isEmpty());

    return OptaplannerSolvers.convert(gso);
  }

  @Override
  public void write(Solution solution, File outputSolutionFile) {
    throw new UnsupportedOperationException();
  }

}
