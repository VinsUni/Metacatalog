//==============================================================================
//===	Copyright (C) 2001-2008 Food and Agriculture Organization of the
//===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
//===	and United Nations Environment Programme (UNEP)
//===
//===	This program is free software; you can redistribute it and/or modify
//===	it under the terms of the GNU General Public License as published by
//===	the Free Software Foundation; either version 2 of the License, or (at
//===	your option) any later version.
//===
//===	This program is distributed in the hope that it will be useful, but
//===	WITHOUT ANY WARRANTY; without even the implied warranty of
//===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//===	General Public License for more details.
//===
//===	You should have received a copy of the GNU General Public License
//===	along with this program; if not, write to the Free Software
//===	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
//===
//===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
//===	Rome - Italy. email: geonetwork@osgeo.org
//==============================================================================

package org.fao.geonet.kernel.search.spatial.deprecated;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.DocIdBitSet;
import org.fao.geonet.kernel.search.spatial.SpatialFilter;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.jdom.Element;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.SpatialOperator;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.SpatialIndex;

import fr.cnes.sitools.metacatalogue.search.solr.spatial.PostgisSpatial;

/**
 * This filter filters out all documents that do not intersect the requested
 * geometry.
 * 
 * @author jeichar
 */
public class FullScanFilter extends SpatialFilter {

	private static final long serialVersionUID = 1114543251684147194L;

	private Set<String> _matches;

	public FullScanFilter(Query query, Element request, Geometry geom,
			FeatureSource<FeatureType, Feature> featureSource,
			SpatialIndex index) throws IOException {
		super(query, geom, featureSource, index);
	}

	protected FullScanFilter(Query query, Envelope bounds,
			FeatureSource featureSource, SpatialIndex index) throws IOException {
		super(query, bounds, featureSource, index);
	}

	@Override
	public DocIdSet getDocIdSet(final IndexReader reader) throws IOException {
		final BitSet bits = new BitSet(reader.maxDoc());

		final Set<String> matches = loadMatches();

		new IndexSearcher(reader).search(_query, new Collector() {
			@Override
			public final void collect(int doc) {
				Document document;
				try {
					document = reader.document(doc, _selector);
					if (matches.contains(document.get("_id"))) {
						bits.set(doc);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void setScorer(Scorer scorer) throws IOException {
			}

			@Override
			public void setNextReader(IndexReader reader, int docBase)
					throws IOException {
			}

			@Override
			public boolean acceptsDocsOutOfOrder() {
				return true;
			}
		});

		return new DocIdBitSet(bits);
	}

	/**
	 * Default for testing purposes
	 * 
	 * @see #createFilter(FeatureSource)
	 * @return the set of ids of the metadata objects that match the geometry of
	 *         this filter
	 * @throws IOException
	 */
	protected synchronized Set<String> loadMatches() throws IOException {
		if (_matches == null) {

			FeatureCollection<SimpleFeatureType, SimpleFeature> features = _featureSource
					.getFeatures(createFilter(_featureSource));
			FeatureIterator<SimpleFeature> iterator = features.features();

			HashSet<String> matches = new HashSet<String>();
			try {
				while (iterator.hasNext()) {
					SimpleFeature feature = iterator.next();
					matches.add((String) feature
							.getAttribute(PostgisSpatial.SPATIAL_INDEX_COLUMN_UUID));
				}
			} finally {
				iterator.close();
			}
			_matches = matches;
		}
		return _matches;
	}

	@Override
	protected SpatialOperator createGeomFilter(FilterFactory2 filterFactory,
			PropertyName geomPropertyName, Literal geomExpression) {
		return filterFactory.disjoint(geomPropertyName, geomExpression);
	}

}
