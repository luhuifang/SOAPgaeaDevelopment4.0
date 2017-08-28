package org.bgi.flexlab.gaea.tools.jointcalling.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bgi.flexlab.gaea.data.structure.location.GenomeLocation;

import htsjdk.tribble.Feature;
import htsjdk.variant.variantcontext.VariantContext;

public class RefMetaDataTracker {
	private GenomeLocation location = null;
	
	public RefMetaDataTracker(){
	}
	
	public GenomeLocation getLocation(){
		return location;
	}
	
	public List<VariantContext> getValues(ArrayList<VariantContext> binding){
		return binding;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Feature> List<T> getValues(RodBinding<VariantContext> binding,GenomeLocation loc){
		return Collections.EMPTY_LIST;
	}
}