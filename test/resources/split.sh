#! /bin/bash

function split(){
    skosFile=$1
    workDir=$2
    mkdir $workDir
    listOfSubjects=`cat $skosFile|grep "Concept> \." |grep -o "^<[^>]*>"|sort|uniq`;
    num=0;
    for i in `echo $listOfSubjects`
    do
	echo "Find statements for concept $i";
	echo "Create $workDir/$num.nt";
	grep "^$i" $skosFile > $workDir/$num.nt;
	num=`expr $num + 1`;
    done
}

#echo "Unzip agrovoc"
#unzip agrovoc_2016-07-15_lod.nt.zip
#echo "Split agrovoc"
#split agrovoc_2016-07-15_lod.nt /tmp/skos-split

#echo "Unzip ddc"
#unzip ddc.nt.zip
#echo "Split ddc"
#split ddc.nt /tmp/ddc-split
