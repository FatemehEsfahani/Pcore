import java.io.FileOutputStream;
import java.io.PrintWriter;
import it.unimi.dsi.io.OutputBitStream;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.Transform;
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.BitStreamArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.GammaCodedLongLabel;
import it.unimi.dsi.webgraph.labelling.Label;
import it.unimi.dsi.webgraph.labelling.LabelMergeStrategy;

public class SymmetrizeWeightedGraphLong {
	
	ArcLabelledImmutableGraph Gu;
	String basename_u;
	int n;
	
	class minLabelMergeStrategy implements LabelMergeStrategy {
		public Label merge(Label first, Label second) {
			if(first == null)
				return second;
			if(second == null)
				return first;
			if(first.getLong()<=second.getLong())
				return first;
			return second;
		}
	}
	
	
	public SymmetrizeWeightedGraphLong(String basename1, String basename2, String basename_u) throws Exception {
		ArcLabelledImmutableGraph G1 = ArcLabelledImmutableGraph.loadMapped(basename1+".w");
		ArcLabelledImmutableGraph G2 = ArcLabelledImmutableGraph.loadMapped(basename2+".w");

		Gu = Transform.union(G1, G2, new minLabelMergeStrategy());
		
		n = Gu.numNodes();
		
		this.basename_u = basename_u;
		
		//union underlying graphs
		ImmutableGraph g1 = ImmutableGraph.loadMapped(basename1);
		ImmutableGraph g2 = ImmutableGraph.loadMapped(basename2);
		ImmutableGraph gu = Transform.union(g1, g2);
		ImmutableGraph.store(BVGraph.class, gu, basename_u);
	}
	
	public void storeUnion() throws Exception {
		
		int STD_BUFFER_SIZE = 1024 * 1024;
		GammaCodedLongLabel prototype = new GammaCodedLongLabel( "FOO" );
		
		final OutputBitStream labels_bitstream = new OutputBitStream( basename_u + ".w" +
				BitStreamArcLabelledImmutableGraph.LABELS_EXTENSION, STD_BUFFER_SIZE ); 
		final OutputBitStream offsets_bitstream = new OutputBitStream( basename_u + ".w" + 
				BitStreamArcLabelledImmutableGraph.LABEL_OFFSETS_EXTENSION, STD_BUFFER_SIZE );

		offsets_bitstream.writeGamma( 0 );

		long count;
		for(int v=0; v<n; v++) {
        	Label[] v_labels = Gu.labelArray(v);
            int v_deg = Gu.outdegree(v);
      
			count = 0;
			for(int i=0; i<v_deg; i++)				
				count += labels_bitstream.writeLongGamma(v_labels[i].getLong());
			
			offsets_bitstream.writeLongGamma( count );
		}
		
		labels_bitstream.close();
		offsets_bitstream.close();
		
		final PrintWriter properties = new PrintWriter( new FileOutputStream( basename_u + ".w" + ImmutableGraph.PROPERTIES_EXTENSION ) );
		properties.println( ImmutableGraph.GRAPHCLASS_PROPERTY_KEY + " = " + BitStreamArcLabelledImmutableGraph.class.getName() );
		properties.println( ArcLabelledImmutableGraph.UNDERLYINGGRAPH_PROPERTY_KEY + " = " + basename_u );
		properties.println( BitStreamArcLabelledImmutableGraph.LABELSPEC_PROPERTY_KEY + " = " + prototype.toSpec() );
		properties.close();
	}
	
	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		
		//args = new String[] {"sssp-test-graph", "sssp-test-graph-t", "sssp-test-graph-u"};
		//args = new String[] {"cnr-2000 cnr-2000-t cnr-2000-u"};
		//args = new String[] {"ljournal-2008 ljournal-2008-t ljournal-2008-u"};
		
		if(args.length != 3 ) {
			System.out.println("Specify: basename basename_t basename_u");
	        System.exit(0);
		}
		
		SymmetrizeWeightedGraphLong t = new SymmetrizeWeightedGraphLong(args[0], args[1], args[2]); 
		t.storeUnion();
		
		System.out.println("Total time elapsed = " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
	}
}
