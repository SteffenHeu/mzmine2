/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.qualityparameters;

import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

/**
 * Calculates quality parameters for each peak in a feature list: - Full width at half maximum
 * (FWHM) - Tailing Factor - Asymmetry factor
 */
public class QualityParameters {

  public static final Logger logger = Logger.getLogger(QualityParameters.class.getName());

  public static void calculateQualityParameters(PeakList peakList) {

    Feature peak;
    double height, rt;

    for (int i = 0; i < peakList.getNumberOfRows(); i++) {
      for (int x = 0; x < peakList.getNumberOfRawDataFiles(); x++) {

        peak = peakList.getPeak(i, peakList.getRawDataFile(x));
        if (peak != null) {
          height = peak.getHeight();
          rt = peak.getRT();

          // FWHM
          double rtValues[] = PeakFindRTs(height / 2, rt, peak);
          Double fwhm = rtValues[1] - rtValues[0];
          if (fwhm <= 0 || Double.isNaN(fwhm) || Double.isInfinite(fwhm)) {
            fwhm = null;
          }
          peak.setFWHM(fwhm);

          // Tailing Factor - TF
          double rtValues2[] = PeakFindRTs(height * 0.05, rt, peak);
          Double tf = (rtValues2[1] - rtValues2[0]) / (2 * (rt - rtValues2[0]));
          if (tf <= 0 || Double.isNaN(tf) || Double.isInfinite(tf)) {
            tf = null;
          }
          peak.setTailingFactor(tf);

          // Asymmetry factor - AF
          double rtValues3[] = PeakFindRTs(height * 0.1, rt, peak);
          Double af = (rtValues3[1] - rt) / (rt - rtValues3[0]);
          if (af <= 0 || Double.isNaN(af) || Double.isInfinite(af)) {
            af = null;
          }
          peak.setAsymmetryFactor(af);

        }
      }
    }

    calculateSNRatios(peakList);

  }

  private static double[] PeakFindRTs(double intensity, double rt, Feature peak) {

    double x1 = 0, x2 = 0, x3 = 0, x4 = 0, y1 = 0, y2 = 0, y3 = 0, y4 = 0, lastDiff1 = intensity,
        lastDiff2 = intensity, currentDiff, currentRT;
    int[] scanNumbers = peak.getScanNumbers();
    RawDataFile dataFile = peak.getDataFile();

    // Find the data points closet to input intensity on both side of the
    // peak apex
    for (int i = 1; i < scanNumbers.length - 1; i++) {

      if (peak.getDataPoint(scanNumbers[i]) != null) {
        currentDiff = Math.abs(intensity - peak.getDataPoint(scanNumbers[i]).getIntensity());
        currentRT = dataFile.getScan(scanNumbers[i]).getRetentionTime();
        if (currentDiff < lastDiff1 & currentDiff > 0 & currentRT <= rt
            & peak.getDataPoint(scanNumbers[i + 1]) != null) {
          x1 = dataFile.getScan(scanNumbers[i]).getRetentionTime();
          y1 = peak.getDataPoint(scanNumbers[i]).getIntensity();
          x2 = dataFile.getScan(scanNumbers[i + 1]).getRetentionTime();
          y2 = peak.getDataPoint(scanNumbers[i + 1]).getIntensity();
          lastDiff1 = currentDiff;
        } else if (currentDiff < lastDiff2 & currentDiff > 0 & currentRT >= rt
            & peak.getDataPoint(scanNumbers[i - 1]) != null) {
          x3 = dataFile.getScan(scanNumbers[i - 1]).getRetentionTime();
          y3 = peak.getDataPoint(scanNumbers[i - 1]).getIntensity();
          x4 = dataFile.getScan(scanNumbers[i]).getRetentionTime();
          y4 = peak.getDataPoint(scanNumbers[i]).getIntensity();
          lastDiff2 = currentDiff;
        }
      }
    }

    // Calculate RT value for input intensity based on linear regression
    double slope, intercept, rt1, rt2;
    if (y1 > 0) {
      slope = (y2 - y1) / (x2 - x1);
      intercept = y1 - (slope * x1);
      rt1 = (intensity - intercept) / slope;
    } else if (x2 > 0) { // Straight drop of peak to 0 intensity
      rt1 = x2;
    } else {
      rt1 = peak.getRawDataPointsRTRange().lowerEndpoint();
    }
    if (y4 > 0) {
      slope = (y4 - y3) / (x4 - x3);
      intercept = y3 - (slope * x3);
      rt2 = (intensity - intercept) / slope;
    } else if (x3 > 0) { // Straight drop of peak to 0 intensity
      rt2 = x3;
    } else {
      rt2 = peak.getRawDataPointsRTRange().upperEndpoint();
    }

    return new double[]{rt1, rt2};
  }


  /**
   * Calculates S/N ratio of features within a given Feature list.
   * <p>
   * Uses 5 * the width of FWHM around the {@link Feature#getRT()} of a feature for calculation of
   * the background noise. The noise is calculated as the standard deviation of the data points
   * within that range. The signal is calculated by (feature height) - (avg intensity in 5 fwhm
   * window).
   * <p>
   * If another feature overlaps the 5 fwhm window before or after a Feature, the range will be
   * expanded to either side. If this is not possible, the s/n ratio is not calculated and 0 is
   * returned.
   *
   * @param featureList
   * @return
   */
  private static void calculateSNRatios(PeakList featureList) {

    for (RawDataFile raw : featureList.getRawDataFiles()) {
      for (Feature feature : featureList.getPeaks(raw)) {

        if(feature == null) {
          continue;
        }

        Range<Double> snRtRange = null;
        Double fwhm = feature.getFWHM();

        if(fwhm == null) {
          feature.setSignalToNoiseRatio(-1.0);
          continue;
        }

        if (fwhm == 0) {
          logger.warning(
              "calcualteSNRatio called without FWHM being set. This might lead to unexpected results.");
        }

        // set to default range
        snRtRange = Range
            .closed(feature.getRawDataPointsRTRange().lowerEndpoint() - 2.5f * feature.getFWHM(),
                feature.getRawDataPointsRTRange().upperEndpoint() + 2.5f * feature.getFWHM());

        snRtRange = fitSnRtRangeToAcquisitionTime(feature, snRtRange);
        if (snRtRange == null) {
          feature.setSignalToNoiseRatio(-1.d);
          continue;
        }

        // Check for other features
        Feature[] otherFeatures = featureList
            .getPeaksInsideMZRange(raw, feature.getRawDataPointsMZRange());

        if (otherFeatures.length != 1) { // more than this feature
          snRtRange = fitRangeToOtherFeatures(feature, otherFeatures, snRtRange);
          if (snRtRange == null) {
            feature.setSignalToNoiseRatio(-1.d);
            continue;
          }
        }

        double height = feature.getHeight();
        List<DataPoint> noiseDP = getDataPointsInRTRange(Range
                .closed(snRtRange.lowerEndpoint(), feature.getRawDataPointsRTRange().lowerEndpoint()),
            feature);
        noiseDP.addAll(getDataPointsInRTRange(Range
                .closed(feature.getRawDataPointsRTRange().upperEndpoint(), snRtRange.upperEndpoint()),
            feature));
        double[] noiseHeights = new double[noiseDP.size()];

        for (int i = 0; i < noiseHeights.length; i++) {
          noiseHeights[i] = noiseDP.get(i).getIntensity();
        }

        double avgNoise = MathUtils.calcAvg(noiseHeights);
        double stdNoise = MathUtils.calcStd(noiseHeights);

        feature.setSignalToNoiseRatio((height - avgNoise) / stdNoise);
        logger.finest("m/z " + feature.getMZ() + " s/n " + feature.getSignalToNoiseRatio());
      }
    }
  }

  /**
   * Fits a given range around a feature (5 FWHM) to the raw data file acquisition range. The might
   * elute early or late.
   *
   * @param feature The feature
   * @param snRange Given range around the feature
   * @return A fitted range or null if the range cannot be fitted.
   */
  @Nullable
  private static Range<Double> fitSnRtRangeToAcquisitionTime(Feature feature,
      Range<Double> snRange) {
    Range<Double> rawRange = feature.getDataFile().getDataRTRange();

    if (rawRange.encloses(snRange)) {
      return snRange;
    } else if (snRange.upperEndpoint() - snRange.lowerEndpoint()
        > rawRange.upperEndpoint() - rawRange.lowerEndpoint()) {
      // 5 fwhm too big
      return null;
    }
    /*else if (snRange.encloses(rawRange)) {
      // 5 fwhm too big
      return null;
    }*/
    else if (snRange.lowerEndpoint() < 0) {
      double shift = snRange.lowerEndpoint();
      snRange = Range.closed(0.0d, snRange.upperEndpoint() + (shift * -1));
    } else if (snRange.upperEndpoint() > rawRange.upperEndpoint()) {
      double shift =
          snRange.upperEndpoint() - rawRange.upperEndpoint();
      snRange = Range.closed(snRange.lowerEndpoint() - shift,
          rawRange.upperEndpoint());
    } else {
//      logger.finest("Cannot fit 5 fwhm range to feature at m/z " + feature.getMZ() + "\n"
//          + " 5 FWHM: " + snRange.toString() + " data file rt range: " + rawRange.toString());
      return null;
    }
    return snRange;
  }

  /**
   * Fits a range around a feature to other features potentially in that range. The range will be
   * shifted to either side if it would overlap with another feature of the same m/z.
   *
   * @param feature       The feature
   * @param otherFeatures Prefiltered array of features in the same m/z range
   * @param snRtRange     The range
   * @return The fitted range or null
   */
  @Nullable
  private static Range<Double> fitRangeToOtherFeatures(final Feature feature,
      final Feature[] otherFeatures,
      Range<Double> snRtRange) {
    final List<Feature> otherFeaturesInRange = new ArrayList<>();
    //check for features in range
    for (Feature otherFeature : otherFeatures) {
      if (feature != otherFeature && (
          snRtRange.contains(otherFeature.getRawDataPointsRTRange().lowerEndpoint()) ||
              snRtRange.contains(otherFeature.getRawDataPointsRTRange().upperEndpoint()))) {
        otherFeaturesInRange.add(otherFeature);
      }
    }

    if (!otherFeaturesInRange.isEmpty()) {
      boolean otherFeaturesOnOneSide = true;
      int move = 0; // -1 means we have to move towards smaller rt, +1 means towards higher rt

      Feature closestFeature = null;

      for (Feature otherFeature : otherFeaturesInRange) {
        int neededMove = (otherFeature.getRT() < feature.getRT()) ? 1 : -1;
        if (move != 0 && neededMove != move) { // this means we cannot move the range
          otherFeaturesOnOneSide = false;
        } else {
          move = neededMove;
          if ((closestFeature != null && otherFeature.getRT() < closestFeature.getRT()) ||
              closestFeature == null) {
            closestFeature = otherFeature;
          }
        }
      }

      if (otherFeaturesOnOneSide == false) {
        return null;
      }

      if (move == 1) {
        double rtMoveOffset =
            closestFeature.getRawDataPointsMZRange().upperEndpoint() - snRtRange
                .lowerEndpoint(); // < 0
        snRtRange = Range.closed(snRtRange.lowerEndpoint() + rtMoveOffset,
            snRtRange.upperEndpoint() + rtMoveOffset);
      } else {
        double rtMoveOffset =
            snRtRange.upperEndpoint() - closestFeature.getRawDataPointsRTRange()
                .lowerEndpoint(); // < 0
        snRtRange = Range.closed(snRtRange.lowerEndpoint() - rtMoveOffset,
            snRtRange.upperEndpoint() - rtMoveOffset);
      }
    }

    if (snRtRange.lowerEndpoint() < 0 || snRtRange.upperEndpoint() > feature.getDataFile()
        .getDataRTRange().upperEndpoint()) {
      // cannot exceed the raw data file's boundaries
      return null;
    }

    if (snRtRange.lowerEndpoint() > feature.getRawDataPointsRTRange().lowerEndpoint()) {
//      logger.finest("lower snrrange > feature range");
      snRtRange = Range
          .closed(feature.getRawDataPointsRTRange().lowerEndpoint(), snRtRange.upperEndpoint());
    } else if (snRtRange.upperEndpoint() < feature.getRawDataPointsRTRange().upperEndpoint()) {
//      logger.finest("upper snrrange < feature range");
      snRtRange = Range
          .closed(snRtRange.lowerEndpoint(), feature.getRawDataPointsRTRange().upperEndpoint());
    }

    return snRtRange;
  }


  /**
   * @param rtRange
   * @param feature
   * @return List of the most intense data points in the given rt range. Uses the m/z range and raw
   * data file of the feature.
   */
  private static List<DataPoint> getDataPointsInRTRange(Range<Double> rtRange, Feature feature) {
    RawDataFile raw = feature.getDataFile();
    Range<Double> mzRange = feature.getRawDataPointsMZRange();
    DataPointSorter sorter = new DataPointSorter(SortingProperty.Intensity,
        SortingDirection.Descending);

    int[] scanNums = raw.getScanNumbers(1, rtRange);

    List<DataPoint> dataPoints = new ArrayList<>();
    for (int i = 0; i < scanNums.length; i++) {
      DataPoint[] dps = raw.getScan(scanNums[i]).getDataPointsByMass(mzRange);
      if (dps != null && dps.length > 0) {
        Arrays.sort(dps, sorter);
        dataPoints.add(dps[0]);
      } else {
        dataPoints.add(new SimpleDataPoint(feature.getMZ(), 0));
      }
    }
    return dataPoints;
  }

}
