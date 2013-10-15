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
package fr.cnes.sitools.metacatalogue.opensearch.extractor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minidev.json.JSONObject;

import org.restlet.Context;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;

import fr.cnes.sitools.metacatalogue.common.Converter;
import fr.cnes.sitools.metacatalogue.common.HarvesterStep;
import fr.cnes.sitools.metacatalogue.common.Metadata;
import fr.cnes.sitools.metacatalogue.exceptions.ProcessException;
import fr.cnes.sitools.metacatalogue.model.Field;
import fr.cnes.sitools.metacatalogue.model.Fields;
import fr.cnes.sitools.metacatalogue.utils.CheckStepsInformation;
import fr.cnes.sitools.metacatalogue.utils.MetacatalogField;
import fr.cnes.sitools.model.AttributeCustom;
import fr.cnes.sitools.model.HarvesterModel;
import fr.cnes.sitools.model.Property;

public class OpensearchMetadataExtractor extends HarvesterStep {

  private String schemaName;

  private Logger logger;

  private HarvesterModel conf;

  private Context context;

  public OpensearchMetadataExtractor(HarvesterModel conf, Context context) {
    this.schemaName = conf.getCatalogType();
    this.conf = conf;
    this.context = context;
    

  }

  @Override
  public void execute(Metadata data) throws ProcessException {
    logger = context.getLogger();
    String metadata = data.getJsonData();

    List<JSONObject> features = JsonPath.read(metadata, "$.features");

    List<Fields> listFields = new ArrayList<Fields>();

    for (JSONObject jsonObject : features) {

      String jsonString = jsonObject.toJSONString();
      Fields fields = new Fields();
      addField(fields, "$.properties.identifier", jsonString, MetacatalogField.ID.getField());
      addField(fields, "$.properties.identifier", jsonString, MetacatalogField._UUID.getField());
      addField(fields, "$.properties.title", jsonString, MetacatalogField.TITLE.getField());
      addField(fields, "$.properties.description", jsonString, MetacatalogField.DESCRIPTION.getField());

      addField(fields, "$.properties.project", jsonString, MetacatalogField.PROJECT.getField());
      addField(fields, "$.properties.product", jsonString, MetacatalogField.PRODUCT.getField());

      addField(fields, "$.properties.platform", jsonString, MetacatalogField.ACQUISITION_SETUP_PLATFORM.getField());
      addField(fields, "$.properties.instrument", jsonString, MetacatalogField.ACQUISITION_SETUP_INSTRUMENT.getField());

      // addField(fields, new Date().toString(), MetacatalogField.MODIFICATION_DATE.getField());

      addField(fields, "$.properties.startDate", jsonString,
          MetacatalogField.CHARACTERISATION_AXIS_TEMPORAL_AXIS_MIN.getField());
      addField(fields, "$.properties.completionDate", jsonString,
          MetacatalogField.CHARACTERISATION_AXIS_TEMPORAL_AXIS_MAX.getField());

      addField(fields, "$.properties.resolution", jsonString, MetacatalogField.ACQUISITION_SETUP_RESOLUTION.getField());
      addField(fields, "$.properties.modified", jsonString, MetacatalogField.MODIFICATION_DATE.getField());

      addField(fields, "$.properties.services.browse.title", jsonString,
          MetacatalogField.SERVICES_BROWSE_TITLE.getField());
      addField(fields, "$.properties.services.browse.layer.type", jsonString,
          MetacatalogField.SERVICES_BROWSE_LAYER_TYPE.getField());
      addField(fields, "$.properties.services.browse.layer.url", jsonString,
          MetacatalogField.SERVICES_BROWSE_LAYER_URL.getField());
      addField(fields, "$.properties.services.browse.layer.layers", jsonString,
          MetacatalogField.SERVICES_BROWSE_LAYER_LAYERS.getField());
      addField(fields, "$.properties.services.browse.layer.version", jsonString,
          MetacatalogField.SERVICES_BROWSE_LAYER_VERSION.getField());
      addField(fields, "$.properties.services.browse.layer.bbox", jsonString,
          MetacatalogField.SERVICES_BROWSE_LAYER_BBOX.getField());
      addField(fields, "$.properties.services.browse.layer.srs", jsonString,
          MetacatalogField.SERVICES_BROWSE_LAYER_SRS.getField());

      addField(fields, "$.properties.services.download.url", jsonString,
          MetacatalogField.DISTRIBUTION_ACCESS_URL.getField());
      addField(fields, "$.properties.services.download.mimeType", jsonString,
          MetacatalogField.DISTRIBUTION_ACCESS_FORMAT.getField());

//      addField(fields, "$.properties.services.metadata.url", jsonString,
//          MetacatalogField.SERVICES_METADATA_URL.getField());

      addField(fields, "$.properties.quicklook", jsonString, MetacatalogField.QUICKLOOK.getField());
      addField(fields, "$.properties.thumbnail", jsonString, MetacatalogField.THUMBNAIL.getField());

      addField(fields, "dataset", MetacatalogField.RESOURCE_TYPE.getField());

      // geometry
      addField(fields, "$.geometry", jsonString, MetacatalogField._GEOMETRY_GEOJSON.getField());

      // public services
      addField(fields, String.valueOf(conf.isPublicServices()), MetacatalogField._PUBLIC_SERVICES.getField());

      // acquisitionSetup
      addAcquisitionSetupField(fields, jsonString);

      OpensearchGeometryExtractor extractor = new OpensearchGeometryExtractor();

      try {
        String geometry = JsonPath.read(jsonString, "$.geometry").toString();
        fields = extractor.extractGeometry(geometry, fields, context);
      }
      catch (Exception e) {
        logger.log(Level.WARNING, e.getMessage(), e);
        throw new ProcessException(e);
      }

      // add the custom attributes
      addCustomAttributes(jsonString, fields, conf.getAttributes());

      listFields.add(fields);

    }

    if (data.getFields() == null) {
      data.setFields(listFields);
    }
    else {
      data.getFields().addAll(listFields);
    }

    if (next != null) {
      next.execute(data);
    }

  }

  private void addCustomAttributes(String json, Fields fields, List<AttributeCustom> attributes) {
    if (attributes != null) {
      for (AttributeCustom attributeCustom : attributes) {
        if (attributeCustom.getValue() != null) {
          fields.add(attributeCustom.getName(), attributeCustom.getValue());
        }
        else {
          Object object = JsonPath.read(json, attributeCustom.getPath());
          if (attributeCustom.getConverterClass() != null) {
            object = applyConverter(object, attributeCustom.getConverterClass(),
                attributeCustom.getConverterParameters());
          }
          fields.add(attributeCustom.getName(), object);
        }
      }
    }
  }

  private void addField(Fields fields, String jsonPath, String json, String fieldName) {
    try {
      Object object = JsonPath.read(json, jsonPath);
      fields.add(fieldName, object);
    }
    catch (InvalidPathException e) {
      logger.log(Level.WARNING, "Invalid path : " + jsonPath, e);
    }
  }

  private void addField(Fields fields, String value, String fieldName) {
    fields.add(fieldName, value);
  }

  private Object applyConverter(Object object, String converterClass, List<Property> converterParameters) {
    try {
      Class<Converter> convClass = (Class<Converter>) Class.forName(converterClass);
      Converter conv = convClass.newInstance();
      object = conv.convert(object, converterParameters);
    }
    catch (ClassNotFoundException e) {
      logger.log(Level.WARNING, "Cannot find converter class : " + converterClass, e);
    }
    catch (InstantiationException e) {
      logger.log(Level.WARNING, "Cannot instanciate converter class : " + converterClass, e);
    }
    catch (IllegalAccessException e) {
      logger.log(Level.WARNING, "Cannot access converter class : " + converterClass, e);
    }
    return object;
  }

  /**
   * Simply add a new acquisitionSetup field, which contains the concatenation of platform and instrument
   * 
   * @param fields
   *          the list of {@link Field}
   */
  private void addAcquisitionSetupField(Fields fields, String json) {
    String plateform = JsonPath.read(json, "$.properties.platform");
    String instrument = JsonPath.read(json, "$.properties.instrument");
    fields.add(MetacatalogField.ACQUISITION_SETUP.getField(), plateform + "/" + instrument);
  }

  @Override
  public void end() throws ProcessException {
    if (next != null) {
      this.next.end();
    }
  }

  @Override
  public CheckStepsInformation check() {
    if (next != null) {
      CheckStepsInformation ok = this.next.check();
      if (!ok.isOk()) {
        return ok;
      }
    }
    return new CheckStepsInformation(true);
  }

}
