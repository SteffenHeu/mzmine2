package net.sf.mzmine.modules.datapointprocessing.setup;

import javafx.scene.control.TreeItem;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingModule;
import net.sf.mzmine.modules.datapointprocessing.ModuleSubCategory;
import net.sf.mzmine.parameters.ParameterSet;

public class DPPModuleTreeItem extends TreeItem<String> {

  private String name;
  private DataPointProcessingModule module;
  private ModuleSubCategory subCat;
  private ParameterSet parameters;
  
  DPPModuleTreeItem(DataPointProcessingModule module){
    setName(module.getName());
    setModule(module);
    setSubCat(module.getModuleSubCategory());
  }

  public String getName() {
    return name;
  }

  public DataPointProcessingModule getModule() {
    return module;
  }

  private void setName(String name) {
    this.name = name;
  }

  private void setModule(DataPointProcessingModule module) {
    this.module = module;
  }

  public ModuleSubCategory getSubCat() {
    return subCat;
  }

  private void setSubCat(ModuleSubCategory subCat) {
    this.subCat = subCat;
  }

  public ParameterSet getParameters() {
    return parameters;
  }

  public void setParameters(ParameterSet parameters) {
    this.parameters = parameters;
  }
}