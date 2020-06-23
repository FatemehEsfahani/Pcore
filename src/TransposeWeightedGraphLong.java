import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.io.OutputBitStream;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.BitStreamArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.GammaCodedLongLabel;
import it.unimi.dsi.webgraph.labelling.Label;

public class TransposeWeightedGraphLong {
	
	String basename;
	String basename_t;
	ArcLabelledImmutableGraph G;
	int n;
	Map<Integer, List<Long>> cache;
	long cache_size = 50_000_000;
	
	public TransposeWeightedGraphLong(String basename) throws Exception {
		this.basename = basename;
		if(basename.endsWith("-t"))
			this.basename_t = basename.substring(0, basename.length() - "-t".length());
		else
			this.basename_t = basename + "-t";
		G = ArcLabelledImmutableGraph.loadMapped(basename+".w");
		n = G.numNodes();
	}
	
	void fillCache(int from, int to) {
		cache = new HashMap<>();
		
        for(int v=0; v<n; v++) {
        	int[] v_neighbors = G.successorArray(v);
        	Label[] v_labels = G.labelArray(v);
            int v_deg = G.outdegree(v);
            
            for(int i=0; i<v_deg; i++) {
            	int u = v_neighbors[i];
            	Label u_label = v_labels[i];
            	
            	if(from<=u & u<to) {
            		if(!cache.containsKey(u))
            			cache.put(u, new ArrayList<>());
            		
            		cache.get(u).add(u_label.getLong());
            	}
            }
        }
	}
	
	public void store() throws Exception {
		
		int STD_BUFFER_SIZE = 1024 * 1024;
		GammaCodedLongLabel prototype = new GammaCodedLongLabel( "FOO" );
		
		final OutputBitStream labels_bitstream = new OutputBitStream( basename_t + ".w" +
				BitStreamArcLabelledImmutableGraph.LABELS_EXTENSION, STD_BUFFER_SIZE ); 
		final OutputBitStream offsets_bitstream = new OutputBitStream( basename_t + ".w" + 
				BitStreamArcLabelledImmutableGraph.LABEL_OFFSETS_EXTENSION, STD_BUFFER_SIZE );

		offsets_bitstream.writeLongGamma( 0 );

		long sum_deg = 0;
		int from = 0;
		for(int v=0; v<n; ) {
			sum_deg += G.outdegree(v);
			
			if(sum_deg > cache_size) {
				System.out.println("Doing vertices from " + from + " to " + v);
				fillCache(from,v);
				saveCache(labels_bitstream, offsets_bitstream, from,v);
				from = v;
				sum_deg = 0;
			}
			else
				v++;
		}
		//last batch
		System.out.println("Doing vertices from " + from + " to " + n);
		fillCache(from,n);
		saveCache(labels_bitstream, offsets_bitstream, from,n);
		
		labels_bitstream.close();
		offsets_bitstream.close();
		
		final PrintWriter properties = new PrintWriter( new FileOutputStream( basename_t + ".w" + ImmutableGraph.PROPERTIES_EXTENSION ) );
		properties.println( ImmutableGraph.GRAPHCLASS_PROPERTY_KEY + " = " + BitStreamArcLabelledImmutableGraph.class.getName() );
		properties.println( ArcLabelledImmutableGraph.UNDERLYINGGRAPH_PROPERTY_KEY + " = " + basename_t );
		properties.println( BitStreamArcLabelledImmutableGraph.LABELSPEC_PROPERTY_KEY + " = " + prototype.toSpec() );
		properties.close();
	}
	
	void saveCache(OutputBitStream labels_bitstream, OutputBitStream offsets_bitstream, int from, int to) throws Exception {
		for(int u=from; u<to; u++) {
			long count = 0;
			
			if(cache.containsKey(u))
				for(Long l : cache.get(u))
					count += labels_bitstream.writeLongGamma(l);
			
			offsets_bitstream.writeLongGamma( count );
		}
	}
	
	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		
		//args = new String[] {"sssp-test-graph"};
		//args = new String[] {"cnr-2000-t"};
		//args = new String[] {"ljournal-2008-noself"};
		
		if(args.length != 1 ) {
			System.out.println("Specify: basename");
	        System.exit(0);
		}
		
		TransposeWeightedGraphLong t = new TransposeWeightedGraphLong(args[0]); 
		t.store();
		
		System.out.println("Total time elapsed = " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
	}
}
