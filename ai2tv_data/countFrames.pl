#!/usr/bin/perl

# Author: Dan Phung (phung@cs.columbia.edu)
# little script I hacked up to get the proportional
# difference between the wf frames and the nowf frames

# should be ran like this: 
# perl countFrame.pl ~pslroot/psl/ai2tv/ai2tv_data/goodness_logs/optimal_start/

# ------------------------------------------------------------------
if ($#ARGV != 0){
    print " Author: Dan Phung (phung@cs.columbia.edu)\n";
    print "\n USAGE: perl countFrames.pl <dir>\n";
    print " <dir> directory holding WF and noWF files\n\n";
    print " EXAMPLE: perl countFrames.pl goodness_logs/optimal_start\n";
    exit;
}

$startDir = $ARGV[0];
# ------------------------------------------------------------------

$wfLogDir = "$startDir/WF";
$nowfLogDir = "$startDir/noWF";
opendir(DIR, $wfLogDir) or die "error, could not open dir: $wfLogDir\n";
 @wfFiles = grep(/\.log$/,readdir(DIR));
closedir(DIR);

opendir(DIR, "$nowfLogDir") or die "error, could not open dir: $nowfLogDir\n";;
 @nowfFiles = grep(/\.log$/,readdir(DIR));
closedir(DIR);

# -------------------
# for stdev calculation for intertrial average
$count = 0;
$sumPropDiff = 0;
$sumSquaredPropDiff = 0;
# -------------------

$wfTotalFrames = 0;
$nowfTotalFrames = 0;
foreach $wfFile (@wfFiles) {
    $nowfFile = shift(nowfFiles);
    # print "comparing $wfFile and $nowfFile\n";
    
    # print "processing file: $file: ";
    $wfFrames = &CountFrames("$wfLogDir/$wfFile");
    $nowfFrames = &CountFrames("$nowfLogDir/$nowfFile");
    $propDiff = ($wfFrames - $nowfFrames) / $nowfFrames;
    $sumPropDiff += $propDiff;
    $sumSquaredPropDiff += ($propDiff * $propDiff);
    $count++;
    print "WF: $wfFrames noWF: $nowfFrames = $propDiff\n";

    $wfTotalFrames += $wfFrames;
    $nowfTotalFrames += $nowfFrames;
}

# -----------------------------------------------------------------
# across all trials
# $propDiff = ($wfTotalFrames - $nowfTotalFrames) / $nowfTotalFrames;
# print "WF: $wfTotalFrames noWF: $nowfTotalFrames = $propDiff\n";

# -----------------------------------------------------------------
# intertrial

$avg = $sumPropDiff / $count;
$avgSquared = $sumSquaredPropDiff / $count;
$variance = ($avgSquared - (($avg *$avg)/$count)) / ($count- 1);
$stdev = sqrt $variance;
print "intertrial average: $avg +\/- $stdev\n";
print "total noWF vs WF frames: $nowfTotalFrames vs $wfTotalFrames\n";

exit;
# -----------------------------------------------------------------
# / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / /
# / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / /
# / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / /
# / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / / /
# -----------------------------------------------------------------
# Subroutines
# -----------------------------------------------------------------
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
	if ($currentFrame =~ m/missed/){
	    # print "found a missed frame: $currentFrame\n";
	    next;
	}
	if ($currentFrame != $lastFrame){
	    # print "$currentFrame";
	    $frameCount++;
	    $lastFrame = $currentFrame;
	} 
    }
    # print "\n" . $frameCount . "\n";
    return $frameCount;
}

