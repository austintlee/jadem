How to build

Right click on build.xml and "Run As" and "2 Ant Build" and select "release"

You can also cd into the working directory where build.xml is located and issue "ant release" on the command line.  
This will create the jar file in both the build directory and the release directory. 
 
How to run

Cd into build and execute:

java -jar taglearner-text-extractor-x.y.jar <input dir> <output dir> <sort type> <output type>

<input dir> = Input directory that contains article-level and OCR XMLs.  
              Make sure the OCR XMLs that articles_xxxx.xml reference are all in the same directory
              E.g.
              Top level input directory
                      18941101
                              articles_18941101.xml
                              0018.xml
                              ...

<output dir> = Output directory to which all extracted text will be written

<sort type> = date | category
              "date" will create a directory for each issue (which corresponds to a single date)
              E.g.
              Top level output directory
                     18941101
                           the_sun_18941101_1_1
                                  the_sun_1894110101_1_1.txt   <---- the file that contains article 1_1
                           ...
              
              "category" will create a directory for each category (which corresponds to a single date)
              E.g.
              Top level output directory
                     sports
                           the_sun_18941101_1_1
                                  the_sun_1894110101_1_1.txt   <---- the file that contains article 1_1
                           ...

<output type> = headline | fulltext
                