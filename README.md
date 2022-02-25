# K-Core Decomposition of Large Probabilistic Graphs
This repository contains efficient implementations for computing the k-core decomposition of large probabilistic graphs. The details of the implementations are described in the following paper:

Fatemeh Esfahani, Venkatesh Srinivasan, Alex Thomo, and Kui Wu: Efficient Computation of Probabilistic Core Decomposition at
Web-Scale. In Proceedings of the 22nd International Conference on Extending Database Technology (EDBT), 325–336. 

# K_BZ
This is an implementation of core decomposition for probabilistic graphs using WebGraph compression framework in:

P. Boldi and S. Vigna. The webgraph framework I: compression techniques. WWW'04

# K_VC
This is an implementation of core decomposition for probabilistic graphs, which do not fit in main memeory, using WebGraph compression framework in:

P. Boldi and S. Vigna. The webgraph framework I: compression techniques. WWW'04

# Compiling

<pre>
mkdir -p bin; javac -cp "bin":"lib/*" -d bin src/it/unimi/dsi/webgraph/labelling/*.java src/*.java
</pre>
Change : to ; if you are on Windows.

# Input for K_BZ and K_VC
The graph datasets for K_BZ and K_VC should be in weighted WebGraph format with edges being assigned probabilities. We refore to this type of Webgraph as ArcLabelled Webgraph. 

There are three files in this format:

newTest.w.labeloffsets<br/>
newTest.w.labels<br/>
newTest.w.properties<br/>

see newTest example in the main directory. 

## Webgraph format
There are many available datasets in http://law.di.unimi.it/datasets.php which can be converted to an ArcLabelled Webgraph. These datasets are unweighted and directed graphs, which are in Webgraph format.

Let us see for an example dataset, cnr-2000, in http://law.di.unimi.it/webdata/cnr-2000

There you can see the follwoing files available for download:

cnr-2000.graph<br/>
cnr-2000.properties<br/>
cnr-2000-t.graph<br/>
cnr-2000-t.properties

...<br/>
(you can ignore the rest of the files)

The first two files are for the forward (regular) cnr-2000 graph. The other two are for the transpose (inverse) graph. 

What is missing is the "offsets" file. This can be easily created by running:

<pre>
java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -o -O -L cnr-2000
</pre>
<pre>
java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -o -O -L cnr-2000-t
</pre>

In our implementations, we assume that the graph deos not have any self-loop. Self-loops can be removed by running:
<pre>
java -Xmx8g -cp "bin":"lib/*" SelfLoopRemover cnr-2000 cnr-2000
</pre>
<pre>
java -Xmx8g -cp "bin":"lib/*" SelfLoopRemover cnr-2000-t cnr-2000-t
</pre>
where the flag "Xmx" specifies the maximum memory allocation pool for a Java virtual machine (JVM). It can be specified in different sizes, such as kilobytes, megabytes, and so on.

Now, we generate weights which are uniformly distibuted. Here, we show this for cnr-2000:
<pre>
java -Xmx8g -cp "bin":"lib/*" GenerateWeightedGraphRandomLong cnr-2000 1 100
</pre>
The above java code produces random weights between range 1 and 100. For each edge, the weight is stored as an integer in the Long format. In our implementations, we access the actual probability of an edge by multplying its corresponding weight by <img src="https://render.githubusercontent.com/render/math?math=10^{-2}">. For instance, for an edge with weight 60, the corresponding probability is obtained by multiplying 60 by 0.01 which is equal to 0.6.

Our implementations work with undirected graphs with symmetrized weights. To change a graph to an undirected one, for each edge we add its inverse. This can be achieved by taking the union of the graph with its transpose. Here, we show how to do this for cnr-2000. Here, we show how to do this for cnr-2000:
<pre>
java -Xmx8g -cp "bin":"lib/*" TransposeWeightedGraphLong cnr-2000 
</pre>
<pre>
java -Xmx8g -cp "bin":"lib/*" SymmetrizeWeightedGraphLong cnr-2000 cnr-2000-t cnr-2000-u
</pre>
The last code creates three files: cnr-2000-u.w.labeloffsets, cnr-2000-u.w.labels, and cnr-2000-u.w.properties. For this dataset, cnr-2000-u.w should be passed as the first input parameter.

Other parametres that are used for ruuning our codes are: &eta;, L, and precision.

We define a threshold &eta; which is used to give certainty when outputting core values in a probabilistic graph. In our implementations, we approximate tail probability of vertex degrees using central limit distribution in statistic. If a vertex has large number of incident edges, the approximation is accurate. The parameter L is used to inidicate where we can use approximation. We set L=1500 in our experiments. In fact, if a vertex has at least L number of incident edges, we approximate the tail probability of that vertex using central limit theorem. Otherwise, we use an exact method. The parameter precision is used to change weights to the actual probabilities. For cnr-2000, precision is equal to 2.

## EdgeList format
This section is for the case when your graph is given a text file of edges (known as edgelist) along with their assigned probabilities. We refer to this file as ''edgelist_weighted.txt'' file.
It is very easy to convert a weighted edgelist file into ArcLabelled Webgraph format. I am making the following assumptions:
1. The graph is unlabeled and the vertices are given by consecutive numbers 0, 1, 2, …
2. The file is TAB separated (not comma separated).

Now, to convert the weighted edgelist file to ArcLabelled Webgraph format execute the following steps:
<pre>
java -Xmx12g -cp "bin":"lib/*" TTextProc edgelist_weighted.txt edgelist_weighted-proc.txt 
</pre>
e.g.
<pre>
java -Xmx12g -cp "bin":"lib/*" TTextProc newTestWeighted.txt newTestWeighted-proc.txt
</pre>
See ''newTest'' example in the main directory. The above code creates a processed version (''edgelist_weighted-proc.txt '') which is symmetric and does not contain any duplicates or self-loops. Moreover, edges are stored up to 16-digit precision. You can change the precision in line 54 of the code to whatever value that you want. Please note that when you change precision, it should be fixed for the rest of the steps.

From your processed file ''edgelist_weighted-proc.txt'', create a text file with the same format as your processed file which contains only edge list (no probabilities). You can call it ''edgelist.txt''.
As an example, ''newTest.txt'' in the main directory is an edge list file which contains edges without their probabilities, and is obtained from ''newTestWeighted-proc.txt''. 

Next step is to create a deterministic Webgraph:

**sort -nk 1 edgelist.txt | uniq > edgelistsortedfile** 

(If you are on Windows, download sort.exe and uniq.exe from http://gnuwin32.sourceforge.net/packages/coreutils.htm)

Run the following:
<pre>
java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -1 -g ArcListASCIIGraph dummy basename < edgelistsortedfile
</pre>
(This will create basename.graph, basename.offsets, basename.properties. The basename can be e.g. ''newTest'').

Next, create a weighted Webgraph using the following:
<pre>
java -Xmx12g -cp "bin":"lib/*" GenerateWeightedGraphFromTxtLong basename edgelist-proc.txt precision
</pre>
This will create three additional files: basename.w.labeloffsets, basename.w.labels, and basename.w.properties.

**Note:** It should be noted that precision is automatically set to 16 significant digits. In line 54, TTextProc.java, you can modify it. 

e.g.
<pre>
java -Xmx12g -cp "bin":"lib/*" GenerateWeightedGraphFromTxtLong newTest newTestWeighted-proc.txt 2
</pre>


The obtained files, **basename.graph**, **basename.offsets**, **basename.properties**, **basename.w.labeloffsets**, **basename.w.labels**, and **basename.w.properties** are required by the algorithms to be able to run. 

# Running
K_BZ:
<pre>
java -Xmx12g -cp "bin":"lib/*" K_BZ basename.w threshold L precision 
</pre>
e.g.
<pre>
java -Xmx12g -cp "bin":"lib/*" K_BZ newTest.w 0.1 1500 2 
</pre>
(Change : to ; if you are on Windows)

The result will be stored in a text file basename+"eta-" + eta + "-bz.txt". The lines of the file are of the form vertex-id:probabilistic core-number.

K_VC:
<pre>
java -Xmx12g -cp "bin":"lib/*" K_VC basename.w threshold L precision  
</pre>
e.g.
<pre>
java -Xmx12g -cp "bin":"lib/*" K_VC newTest.w 0.1 1500 2  
</pre>
The result will be stored in a text file basename+"eta-" + eta + "-vc.txt". The lines of the file are of the form vertex-id:probabilistic core-number.

# Using git
First clone repo.

<pre>
git clone https://github.com/FatemehEsfahani/Probabilistic-Core-Decomposition.git
</pre>

This will create a directory "Probabilistic-Core-Decomposition" with the current code of this project. The subdirectories created are "src" and "lib". 

Copy the source files you changed to "Probabilistic-Core-Decomposition/src". 

While being in "Probabilistic-Core-Decomposition", run 
<pre>
git add .
</pre>

Commit changes by running
<pre>
git commit -m "some comment about your changes"
</pre>
If it is the first time, you will be required to run two other commands before. 
<pre>
git config --global user.email "your email"
git config --global user.name "your name"
</pre>

Finally, run
<pre>
git push
</pre>

You will be required to specify username and password. 
If successful, the changes will be in the repository.

