/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import java.io.Serializable;
import java.util.ArrayList;

import javax.annotation.Nullable;

import com.github.rinde.jaamas16.PerformExperiment.AuctionStats;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel.AuctionEvent;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionTimeStatsLogger;
import com.github.rinde.logistics.pdptw.mas.comm.Bidder;
import com.github.rinde.logistics.pdptw.mas.route.RoutePlanner;
import com.github.rinde.logistics.pdptw.mas.route.RoutePlannerStatsLogger;
import com.github.rinde.rinsim.central.SolverTimeMeasurement;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger.LogEntry;
import com.github.rinde.rinsim.core.model.time.RealtimeTickInfo;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.PostProcessor;
import com.github.rinde.rinsim.experiment.PostProcessors;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;

class JaamasPostProcessor implements PostProcessor<SimResult>, Serializable {
  private static final long serialVersionUID = 5997690791395717045L;
  ObjectiveFunction objectiveFunction;

  JaamasPostProcessor(ObjectiveFunction objFunc) {
    objectiveFunction = objFunc;
  }

  @Override
  public SimResult collectResults(Simulator sim, SimArgs args) {

    @Nullable
    final RealtimeClockLogger logger =
      sim.getModelProvider().tryGetModel(RealtimeClockLogger.class);

    @Nullable
    final AuctionCommModel<?> auctionModel =
      sim.getModelProvider().tryGetModel(AuctionCommModel.class);

    final Optional<AuctionStats> aStats;
    if (auctionModel == null) {
      aStats = Optional.absent();
    } else {
      final int parcels = auctionModel.getNumParcels();
      final int reauctions = auctionModel.getNumAuctions() - parcels;
      final int unsuccessful = auctionModel.getNumUnsuccesfulAuctions();
      final int failed = auctionModel.getNumFailedAuctions();
      aStats = Optional
        .of(AuctionStats.create(parcels, reauctions, unsuccessful, failed));
    }

    final StatisticsDTO stats =
      PostProcessors.statisticsPostProcessor(objectiveFunction)
        .collectResults(sim, args);

    PerformExperiment.LOGGER.info("success: {}", args);

    @Nullable
    final AuctionTimeStatsLogger auctionLogger =
      sim.getModelProvider().tryGetModel(AuctionTimeStatsLogger.class);
    ImmutableList<AuctionEvent> finishEvents;
    ImmutableListMultimap<Bidder<?>, SolverTimeMeasurement> timeMeasurements;
    if (auctionLogger != null) {
      finishEvents = auctionLogger.getAuctionFinishEvents();
      timeMeasurements = auctionLogger.getTimeMeasurements();
    } else {
      finishEvents = ImmutableList.of();
      timeMeasurements = ImmutableListMultimap.of();
    }

    @Nullable
    final RoutePlannerStatsLogger rpLogger =
      sim.getModelProvider().tryGetModel(RoutePlannerStatsLogger.class);
    ImmutableListMultimap<RoutePlanner, SolverTimeMeasurement> rpTimeMeasurements;
    if (auctionLogger != null) {
      rpTimeMeasurements = rpLogger.getTimeMeasurements();
    } else {
      rpTimeMeasurements = ImmutableListMultimap.of();
    }

    if (logger == null) {
      return SimResult.create(new ArrayList<LogEntry>(), 0,
        sim.getCurrentTime() / sim.getTimeStep(), stats,
        ImmutableList.<RealtimeTickInfo>of(), aStats,
        finishEvents,
        timeMeasurements, rpTimeMeasurements);
    }
    return SimResult.create(logger.getLog(), logger.getRtCount(),
      logger.getStCount(), stats, logger.getTickInfoList(), aStats,
      finishEvents, timeMeasurements, rpTimeMeasurements);
  }

  @Override
  public FailureStrategy handleFailure(Exception e, Simulator sim,
      SimArgs args) {

    System.out.println("Fail: " + args);
    e.printStackTrace();
    // System.out.println(AffinityLock.dumpLocks());

    return FailureStrategy.RETRY;
    // return FailureStrategy.ABORT_EXPERIMENT_RUN;

  }
}
