package net.sf.mzmine.modules.rawdatamethods.filtering.badexportfilter;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineRunnableModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class BadExportFilterModule implements MZmineRunnableModule {

  @Nonnull
  @Override
  public String getDescription() {
    return "Filters by scan width to detect full and MRM scans to put them into individual mass lists.";
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    tasks.add(new BadExportFilterTask((SimpleParameterSet)parameters));

    return ExitCode.OK;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.RAWDATA;
  }

  @Nonnull
  @Override
  public String getName() {
    return "Bad export filter";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return BadExportFilterParameters.class;
  }
}
