#! /bin/bash

workDir="/tmp/skos-split"
unzip agrovoc_2016-07-15_lod.nt.zip
mkdir $workDir

listOfSubjects=`cat agrovoc_2016-07-15_lod.nt|grep "Concept> \." |grep -o "^<[^>]*>"|sort|uniq`;
num=0;
for i in `echo $listOfSubjects`
do
    echo "Find statements for concept $i";
    echo "Create $workDir/$num.nt";
    grep "^$i" agrovoc_2016-07-15_lod.nt > $workDir/$num.nt;
    num=`expr $num + 1`;
done
