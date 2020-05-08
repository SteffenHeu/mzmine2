package net.sf.mzmine.modules.rawdatamethods.filtering.badexportfilter;

import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class BadExportFilterParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final MZToleranceParameter mzRangeTolerance = new MZToleranceParameter(
      "m/z scan width tolerance",
      "The tolerance for m/z deviation between scans to be put in the same mass list");

  public static final DoubleParameter mrmScanWidth = new DoubleParameter("Maximum MSMS scan width",
      "Maximum width of a scan to be classified as a MSMS scan.");

  public static final DoubleParameter precursorMZ = new DoubleParameter("Precursor m/z",
      "");

  public static final IntegerParameter precursorCharge = new IntegerParameter("Precursor charge",
      "");

  public BadExportFilterParameters() {
    super(new UserParameter[]{dataFiles, mrmScanWidth, precursorMZ, precursorCharge});
  }
}
