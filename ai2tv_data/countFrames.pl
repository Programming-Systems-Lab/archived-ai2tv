#!/usr/bin/perl

# CVS version control block - do not edit manually
#  $RCSfile$
#  $Revision$
#  $Date$
#  $Source$
#
# Author: Dan Phung (phung@cs.columbia.edu)
# little script I hacked up to get the proportional
# difference between the wf frames and the nowf frames
#
# should be ran in the directory:
# c:/pslroot/psl/ai2tv/ai2tv_data/goodness_logs/optimal_start/

$wfLogDir = "WF";
$nowfLogDir = "noWF";
opendir(DIR, $wfLogDir) or die "error, could not open dir: $wfLogDir\n";
 @wfFiles = grep(/\.log$/,readdir(DIR));
closedir(DIR);

opendir(DIR, "$nowfLogDir") or die "error, could not open dir: $nowfLogDir\n";;
 @nowfFiles = grep(/\.log$/,readdir(DIR));
closedir(DIR);

$wfTotalFrameCount = 0;
$nowfTotalFrameCount = 0;
$count = 0;
$sumProps = 0;
foreach $wfFile (@wfFiles) {
    $nowfFile = shift(nowfFiles);
    # print "comparing $wfFile and $nowfFile\n";
    
    # print "processing file: $file: ";
    $wfFrameCount = &CountFrames("$wfLogDir/$wfFile");
    $nowfFrameCount = &CountFrames("$nowfLogDir/$nowfFile");
    $propDiff = ($wfFrameCount - $nowfFrameCount) / $nowfFrameCount;
    $sumProps += $propDiff;
    $count++;
    # print "WF: $wfFrameCount noWF: $nowfFrameCount = $propDiff\n";

    $wfTotalFrameCount += $wfFrameCount;
    $nowfTotalFrameCount += $nowfFrameCount;
    # if ($count++ == 2){ exit;}
}

$propDiff = ($wfTotalFrameCount - $nowfTotalFrameCount) / $nowfTotalFrameCount;
print "WF: $wfTotalFrameCount noWF: $nowfTotalFrameCount = $propDiff\n";
print "intertrial average: " . $sumProps / $count. "\n";

sub CountFrames(){
    my $file = shift;
    # print "counting frames for $file: ";
    open(IN, $file) or die "Error, could not open $file\n";
    my $count = 0;
    my $lastFrame = 0;
    my $frameCount = 0;
    while(<IN>){
	# skip the first header line
	if ($count++ == 0){ next; }

	my @line = split (/\t/, $_);	
	$currentFrame = $line[5];
	if ($currentFrame != $lastFrame){
	    # print "$currentFrame";
	    $frameCount++;
	    $lastFrame = $currentFrame;
	} 
    }
    # print "\n" . $frameCount . "\n";
    return $frameCount;
}

