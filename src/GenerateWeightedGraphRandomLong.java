import java.io.FileOutputStream;
import java.io.PrintWriter;

import it.unimi.dsi.io.OutputBitStream;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.BitStreamArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.GammaCodedLongLabel;

public class GenerateWeightedGraphRandomLong {

	ImmutableGraph G;
    int n;
	long m;
	long minWeight; 
	long maxWeight; 
	String basename;
	
	public GenerateWeightedGraphRandomLong(String basename, long minWeight, long maxWeight) throws Exception {
		
		this.basename = basename;
		this.minWeight = minWeight;
		this.maxWeight = maxWeight;
		
		this.G = ImmutableGraph.loadMapped(basename);
	}
	
	public void store() throws Exception {
		int STD_BUFFER_SIZE = 1024 * 1024;
		ProgressLogger pl = new ProgressLogger();
		GammaCodedLongLabel prototype = new GammaCodedLongLabel( "FOO" );
		GammaCodedLongLabel label = prototype.copy();
		
		final OutputBitStream labels = new OutputBitStream( basename + ".w" +
				BitStreamArcLabelledImmutableGraph.LABELS_EXTENSION, STD_BUFFER_SIZE ); 
		final OutputBitStream offsets = new OutputBitStream( basename + ".w" + 
				BitStreamArcLabelledImmutableGraph.LABEL_OFFSETS_EXTENSION, STD_BUFFER_SIZE );

		if ( pl != null ) {
			pl.itemsName = "nodes";
			pl.expectedUpdates = G.numNodes();
			pl.start( "Saving labels..." );
		}

		final NodeIterator nodeIterator = G.nodeIterator();
		offsets.writeGamma( 0 );
		int curr;
		long count;
		LazyIntIterator successors;

		while( nodeIterator.hasNext() ) {
			curr = nodeIterator.nextInt();
			successors = nodeIterator.successors();
			count = 0;
			while( successors.nextInt() != -1 ) {
				label.value = (long)(minWeight + (maxWeight-minWeight)*Math.random()); 
				count += label.toBitStream( labels, curr );
			}
			offsets.writeLongGamma( count );
			if ( pl != null ) pl.lightUpdate();
		}
		
		if ( pl != null ) pl.done();
		labels.close();
		offsets.close();
		
		final PrintWriter properties = new PrintWriter( new FileOutputStream( basename + ".w" + ImmutableGraph.PROPERTIES_EXTENSION ) );
		properties.println( ImmutableGraph.GRAPHCLASS_PROPERTY_KEY + " = " + BitStreamArcLabelledImmutableGraph.class.getName() );
		properties.println( ArcLabelledImmutableGraph.UNDERLYINGGRAPH_PROPERTY_KEY + " = " + basename );
		properties.println( BitStreamArcLabelledImmutableGraph.LABELSPEC_PROPERTY_KEY + " = " + prototype.toSpec() );
		properties.close();
	}
	
	public static void main(String[] args) throws Exception {
		//args = new String[] {"uk-2005", "0", "100"};
		
		if(args.length != 3 ) {
			System.out.println("Specify: basename minWeight maxWeight");
	        System.exit(0);
		}
			
		GenerateWeightedGraphRandomLong gen = new GenerateWeightedGraphRandomLong(args[0], Long.parseLong(args[1]), Long.parseLong(args[2]));
		gen.store();
	}

}
