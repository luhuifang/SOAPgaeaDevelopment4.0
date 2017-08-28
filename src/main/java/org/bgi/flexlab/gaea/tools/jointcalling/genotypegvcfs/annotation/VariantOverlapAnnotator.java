package org.bgi.flexlab.gaea.tools.jointcalling.genotypegvcfs.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.bgi.flexlab.gaea.data.structure.location.GenomeLocation;
import org.bgi.flexlab.gaea.data.structure.location.GenomeLocationParser;
import org.bgi.flexlab.gaea.tools.jointcalling.util.RefMetaDataTracker;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.vcf.VCFConstants;

public final class VariantOverlapAnnotator {
	final ArrayList<VariantContext> dbSNPBinding;
    final Map<String,ArrayList<VariantContext>> overlapBindings;
    final GenomeLocationParser genomeLocParser;
    
    /**
     * Create a new VariantOverlapAnnotator
     *
     * @param dbSNPBinding the RodBinding to use for updating ID field values, or null if that behavior isn't desired
     * @param overlapBindings a map of RodBindings / name to use for overlap annotation.  Each binding will be used to
     *                        add name => true for variants that overlap with variants found to a
     *                        RefMetaDataTracker at each location.  Can be empty but not null
     * @param genomeLocationParser the genome location parser we'll use to create GenomeLocs for VariantContexts
     */
    public VariantOverlapAnnotator(ArrayList<VariantContext> dbSNPBinding, Map<String, ArrayList<VariantContext>> overlapBindings, GenomeLocationParser genomeLocParser) {
        if ( genomeLocParser == null ) throw new IllegalArgumentException("genomeLocationParser cannot be null");
        if ( overlapBindings == null ) throw new IllegalArgumentException("overlapBindings cannot be null");
        
        this.dbSNPBinding = dbSNPBinding;
        this.overlapBindings = overlapBindings;
        this.genomeLocParser = genomeLocParser;
    }

    /**
     * Get the collection of the RodBinding names for those being used for overlap detection
     * @return a non-null collection of Strings
     */
    public Collection<String> getOverlapNames() {
        return overlapBindings.keySet();
    }
    
    public VariantContext annotateRsID(final RefMetaDataTracker tracker, final VariantContext vcToAnnotate) {
        if ( dbSNPBinding != null ) {
            final GenomeLocation loc = getLocation(vcToAnnotate);
            return annotateRsID(tracker.getValues(dbSNPBinding, loc), vcToAnnotate);
        } else {
            return vcToAnnotate;
        }
    }
    
    private GenomeLocation getLocation(final VariantContext vc) {
        return genomeLocParser.createGenomeLocation(vc);
    }
    
    /**
     * Update rsID of vcToAnnotate with rsID match found in vcsAtLoc, if one exists
     *
     * @param vcsAtLoc a list of variant contexts starting at this location to use as sources for rsID values
     * @param vcToAnnotate a variant context to annotate
     * @return a VariantContext (may be == to vcToAnnotate) with updated rsID value
     */
    public VariantContext annotateRsID(final List<VariantContext> vcsAtLoc, final VariantContext vcToAnnotate ) {
        final String rsID = getRsID(vcsAtLoc, vcToAnnotate);

        // add the ID if appropriate
        if ( rsID != null ) {
            final VariantContextBuilder vcb = new VariantContextBuilder(vcToAnnotate);

            if ( ! vcToAnnotate.hasID() ) {
                return vcb.id(rsID).make();
            } else if ( ! vcToAnnotate.getID().contains(rsID) ) {
                return vcb.id(vcToAnnotate.getID() + VCFConstants.ID_FIELD_SEPARATOR + rsID).make();
            } // falling through to return VC lower down
        }

        // nothing to do, just return vc
        return vcToAnnotate;
    }
    
    private String getRsID(final List<VariantContext> rsIDSourceVCs, final VariantContext vcToAnnotate) {
        if ( rsIDSourceVCs == null ) throw new IllegalArgumentException("rsIDSourceVCs cannot be null");
        if ( vcToAnnotate == null ) throw new IllegalArgumentException("vcToAnnotate cannot be null");

        for ( final VariantContext vcComp : rsIDSourceVCs ) {
            if ( vcComp.isFiltered() ) continue; // don't process any failed VCs

            if ( ! vcComp.getContig().equals(vcToAnnotate.getContig()) || vcComp.getStart() != vcToAnnotate.getStart() )
                throw new IllegalArgumentException("source rsID VariantContext " + vcComp + " doesn't start at the same position as vcToAnnotate " + vcToAnnotate);

            if ( vcToAnnotate.getReference().equals(vcComp.getReference()) ) {
                for ( final Allele allele : vcToAnnotate.getAlternateAlleles() ) {
                    if ( vcComp.getAlternateAlleles().contains(allele) )
                        return vcComp.getID();
                }
            }
        }

        return null;
    }
    
    public VariantContext annotateOverlaps(final RefMetaDataTracker tracker, final VariantContext vcToAnnotate) {
        if ( overlapBindings.isEmpty() ) return vcToAnnotate;

        VariantContext annotated = vcToAnnotate;
        final GenomeLocation loc = getLocation(vcToAnnotate);
        for ( final Map.Entry<String, ArrayList<VariantContext>> overlapBinding : overlapBindings.entrySet() ) {
            annotated = annotateOverlap(tracker.getValues(overlapBinding.getValue(), loc), overlapBinding.getKey(), annotated);
        }

        return annotated;
    }
    
    public VariantContext annotateOverlap(final List<VariantContext> overlapTestVCs, final String attributeKey, VariantContext vcToAnnotate) {
        if ( overlapBindings.isEmpty() ) return vcToAnnotate;

        final boolean overlaps = overlaps(overlapTestVCs, vcToAnnotate);
        if ( overlaps ) {
            return new VariantContextBuilder(vcToAnnotate).attribute(attributeKey, true).make();
        } else {
            return vcToAnnotate;
        }
    }
    
    private boolean overlaps(final List<VariantContext> potentialOverlaps, final VariantContext vcToAnnotate) {
        return getRsID(potentialOverlaps, vcToAnnotate) != null;
    }
}
