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

# Input for K_BZ and K_VC
The graphs for K_BZ and K_VC should be in WebGraph format with edges being assigned probabilities. We refore to this type of Webgraph as ArcLabelled Webgraph. 

There are three files in this format:

newTest.w.labeloffsets<br/>
newTest.w.labels<br/>
newTest.w.properties<br/>

see newTest example in the main directory. 

There are many available datasets in http://law.di.unimi.it/datasets.php which can be converted to an ArcLabelled Webgraph. These datasets are unweighted and directed graphs.

Let us see for an example dataset, cnr-2000, in http://law.di.unimi.it/webdata/cnr-2000

There you can see the follwoing files available for download:

cnr-2000.graph<br/>
cnr-2000.properties<br/>
cnr-2000-t.graph<br/>
cnr-2000-t.properties<br/>

...<br/>
(you can ignore the rest of the files)



# Edgelist format
This section is for the case when your graph is given a text file of edges (known as edgelist). If your graph is already in WebGraph format, skip to the next section.

It is very easy to convert an edgelist file into WebGraph format. I am making the folloiwng assumptions:

1) The graph is unlabeled and the vertices are given by consecutive numbers, 0,1,2,...
(If there are some vertices "missing", e.g. you don't have a vertex 0 in your file, it's not a problem. WebGraph will create dummy vertices, e.g. 0, that does not have any neighbor.)

2) The edgelist file is TAB separated (not comma separated). The last element of each line is edge probabilty which is stored in Long format.

# Compiling

<pre>
mkdir -p bin; javac -cp "bin":"lib/*" -d bin src/it/unimi/dsi/webgraph/labelling/*.java src/*.java
</pre>

# Running
K_BZ:

java -Xmx12g -cp "bin":"lib/*" K_BZ basename.w threshold L precision DPtype 

e.g.
java -Xmx12g -cp "bin":"lib/*" K_BZ newTest.w 0.1 100 2 

(Change : to ; if you are on Windows)

The result will be stored in a text file basename+"eta-" + eta + "-bz.txt". The lines of the file are of the form vertex-id:core-number.

K_VC:

java -Xmx12g -cp "bin":"lib/*" K_VC basename.w threshold L precision DPtype 

e.g.
java -Xmx12g -cp "bin":"lib/*" K_VC newTest.w threshold L precision DPtype 

The result will be stored in a text file basename+"eta-" + eta + "-vc.txt". The lines of the file are of the form vertex-id:core-number.

# Using git
First clone repo.

<pre>
git clone https://github.com/thomouvic/pcore.git
</pre>

This will create a directory "pcore" with the current code of this project. The subdirectories created are "src" and "lib". 

Copy the source files you changed to "pcode/src". 

While being in "pcode", run 
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

