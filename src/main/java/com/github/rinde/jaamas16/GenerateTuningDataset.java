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

import static java.util.Arrays.asList;

import com.github.rinde.datgen.pdptw.DatasetGenerator;

/**
 *
 * @author Rinde van Lon
 */
public final class GenerateTuningDataset {

  private GenerateTuningDataset() {}

  /**
   * @param args Ignored.
   */
  public static void main(String[] args) {
    final long time = System.currentTimeMillis();
    final DatasetGenerator generator = DatasetGenerator.builder()
      .setRandomSeed(201611181750L)
      .setDatasetDir("files/tuning-dataset/")
      .setDynamismLevels(asList(.2, .5, .8))
      .setUrgencyLevels(asList(5L, 20L, 35L))
      .setScaleLevels(asList(10d))
      .setNumInstances(5)
      .build();

    generator.generate();
    final long duration = System.currentTimeMillis() - time;
    System.out.println("Done, in " + duration / 1000d + "s");

  }

}
