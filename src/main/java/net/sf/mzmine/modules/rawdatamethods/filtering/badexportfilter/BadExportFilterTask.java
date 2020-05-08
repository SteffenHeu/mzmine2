package net.sf.mzmine.modules.rawdatamethods.filtering.badexportfilter;

import com.google.common.collect.Range;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzDataReadTask;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import org.checkerframework.checker.nullness.qual.Raw;

public class BadExportFilterTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(BadExportFilterTask.class.getName());

  private final SimpleParameterSet parameters;
  private final RawDataFile[] rawDataFiles;
  //  private final MZTolerance mzTolerance;
  private final double mrmScanWidth;
  private final double precursorMZ;
  private final int precursorCharge;

  public BadExportFilterTask(SimpleParameterSet parameters) {
    this.parameters = parameters;
    rawDataFiles = parameters.getParameter(BadExportFilterParameters.dataFiles).getValue()
        .getMatchingRawDataFiles();
//    mzTolerance = parameters.getParameter(BadExportFilterParameters.mzRangeTolerance).getValue();
    mrmScanWidth = parameters.getParameter(BadExportFilterParameters.mrmScanWidth).getValue();
    precursorMZ = parameters.getParameter(BadExportFilterParameters.precursorMZ).getValue();
    precursorCharge = parameters.getParameter(BadExportFilterParameters.precursorCharge).getValue();
  }

  @Override
  public String getTaskDescription() {
    return "Bad export filter task";
  }


  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    logger.info("Bad export filter started.");

    setStatus(TaskStatus.PROCESSING);

    List<RawDataFile> finalRaws = new ArrayList<>();

    for (int i = 0; i < rawDataFiles.length; i++) {
      RawDataFile file = rawDataFiles[i];

      RawDataFileImpl newMZmineFile;
      try {
        newMZmineFile = new RawDataFileImpl(file + " reprocessed");
      } catch (IOException e) {
        e.printStackTrace();
        continue;
      }

      int scanNums[] = file.getScanNumbers();
      for (int j = 0; j < scanNums.length; j++) {
        int scanNum = scanNums[j];
        Scan scan = file.getScan(scanNum);
        int msLevel = 1;
        double rt = scan.getRetentionTime();
        DataPoint[] dps = scan.getDataPoints();
        MassSpectrumType massSpectrumType = scan.getSpectrumType();
        ;
        PolarityType polarityType = PolarityType.POSITIVE;
        ;
        String scanDefinition = scan.getScanDefinition();
        Range<Double> mzScanRange = scan.getScanningMZRange();
        int precursorCharge = 0;

        if (scan.getDataPointMZRange().upperEndpoint() - scan.getDataPointMZRange().lowerEndpoint()
            < mrmScanWidth) {
          msLevel = 2;
          precursorCharge = this.precursorCharge;
        }

        Scan properScan = new SimpleScan(newMZmineFile, scanNum, msLevel, rt, precursorMZ,
            precursorCharge, null, dps, massSpectrumType, polarityType, scanDefinition,
            mzScanRange);

        try {
          newMZmineFile.addScan(properScan);
        } catch (IOException e) {
          logger.log(Level.SEVERE, "Could not add scan. " + e.getMessage());
          e.printStackTrace();
        }

      }

      try {
        RawDataFile newFile = newMZmineFile.finishWriting();
        MZmineCore.getProjectManager().getCurrentProject().addFile(newFile);
        MZmineCore.getProjectManager().getCurrentProject().removeFile(file);
      } catch (IOException e) {
        e.printStackTrace();
      }

    }

    setStatus(TaskStatus.FINISHED);

  }
}
