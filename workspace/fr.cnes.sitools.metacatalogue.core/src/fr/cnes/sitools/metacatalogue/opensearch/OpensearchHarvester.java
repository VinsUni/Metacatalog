/*******************************************************************************
 * Copyright 2011 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 * 
 * This file is part of SITools2.
 * 
 * SITools2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * SITools2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with SITools2.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.cnes.sitools.metacatalogue.opensearch;

import java.util.logging.Level;

import org.restlet.Context;

import fr.cnes.sitools.metacatalogue.common.Harvester;
import fr.cnes.sitools.metacatalogue.common.HarvesterStep;
import fr.cnes.sitools.metacatalogue.csw.validator.CswMetadataValidator;
import fr.cnes.sitools.metacatalogue.exceptions.CheckProcessException;
import fr.cnes.sitools.metacatalogue.exceptions.ProcessException;
import fr.cnes.sitools.metacatalogue.opensearch.extractor.OpensearchMetadataExtractor;
import fr.cnes.sitools.metacatalogue.opensearch.indexer.OpensearchMetadataIndexer;
import fr.cnes.sitools.metacatalogue.opensearch.reader.OpensearchReader;
import fr.cnes.sitools.metacatalogue.utils.CheckStepsInformation;
import fr.cnes.sitools.metacatalogue.utils.HarvesterSettings;
import fr.cnes.sitools.model.HarvesterModel;

public class OpensearchHarvester extends Harvester {

  private HarvesterStep step1;

  private Context context;

  @Override
  public void initHarvester(HarvesterModel harvestConf, Context context) throws CheckProcessException {
    super.initHarvester(harvestConf, context);
    HarvesterStep step2, step3, step4;
    step1 = new OpensearchReader(harvestConf, context);
    step2 = new OpensearchMetadataExtractor(harvestConf, context);
    step3 = new CswMetadataValidator(harvestConf, context);
    step4 = new OpensearchMetadataIndexer(harvestConf, context);

    step1.setNext(step2);
    step2.setNext(step3);
    step3.setNext(step4);

    CheckStepsInformation ok = step1.check();
    if (!ok.isOk()) {
      throw new CheckProcessException(ok.getMessage());
    }
    this.context = context;
  }

  @Override
  public void harvest() throws Exception {
    try {
      step1.execute(null);
    }
    catch (ProcessException e) {
      HarvesterSettings.getInstance().getLogger().log(Level.WARNING, e.getMessage(), e);
      throw e;
    }
  }

}