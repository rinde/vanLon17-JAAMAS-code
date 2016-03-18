library("tikzDevice")
library("ggplot2")
library("data.table")
library("reshape2")
library(plyr)

script.dir <- dirname(sys.frame(1)$ofile)
source(paste(script.dir,"multiplot.r",sep="/"))
target.dir <- paste(script.dir,"/../../../files/results/BEST/GENDREAU/",sep="")

# 2016-01-15T15/19/08-Optaplanner-Benchmark

# arg list: [includeGendreau, dir1, dir2, ..., dirn ]


dirs <- c(#"2016-01-15T15:19:08-Optaplanner-Benchmark-part1",
          #"2016-01-16T21:02:54-Optaplanner-Benchmark-part2")
          #"2016-01-19T17:21:46-Optaplanner-Benchmark-round2")
          #"2016-01-22T10:46:25-Optaplanner-Benchmark-cont-update-round1")
          #"2016-01-29T18:31:26-Optaplanner-benchmark-3rep")
          #"2016-02-19T11:42:18-MAS")
          "2016-03-08T11:10:25-MAS-Benchmark")

files <- list()# list("2016-01-04T18:03:53-OFFLINE/Optaplanner-validated-600s.csv")
for( dir in dirs){
  temp.files <- list.files(paste(target.dir,dir,sep=""),pattern=".*-final.csv",recursive=T)
  temp.files <- paste(dir,"/",temp.files,sep="")
  if( is.null(files)){
    files <- temp.files
  } else {
    files <- append(files,temp.files)
  }
}



# files <- c(
#   
#   "2016-01-04T18:03:53-OFFLINE/Optaplanner-validated-600s.csv",
#   "2016-01-04T17:40:57-OFFLINE/Optaplanner-validated-30s.csv",
#   "2016-01-04T17:23:41-OFFLINE/Optaplanner-validated-10s.csv",
#   
#   #"2015-12-02T19:55:45-OFFLINE/Central-Opt2Bfs(GendrOF(30.0))-final.csv",
#   "2015-12-04T16:03:02/RtCentral-Opt2BfsRT(GendrOF(30.0))-final.csv",
#   "2015-12-09T17:23:37/RtCentral-Opt2BfsRT(GendrOF(30.0))-final.csv",
#   
#   "2015-12-04T16:03:02/ReAuction-2optRP-cihBID-final.csv",
#    #"2015-11-30/RtCentral-Opt2BfsRT(GendrOF(30.0))-final.csv",
#   #"2015-12-04T15:12:52/ReAuction-2optRP-cihBID-final.csv",
#  
# #  "2015-12-09T11:42:04/ReAuction-2optRP-cihBID-final.csv",
# #  "2015-12-09T14:16:57/ReAuction-2optRP-cihBID-final.csv",
#   "2015-12-09T17:23:37/ReAuction-2optRP-cihBID-final.csv",
#   
# #  "2015-12-09T14:16:57/ReAuction-2optRP-cihBID-BAL-HIGH-final.csv",
# #  "2015-12-09T11:42:04/ReAuction-2optRP-cihBID-BAL-HIGH-final.csv",
#   "2015-12-09T17:23:37/ReAuction-2optRP-cihBID-BAL-HIGH-final.csv",
#   
# #  "2015-12-09T11:42:04/ReAuction-2optRP-cihBID-BAL-LOW-final.csv",
# #  "2015-12-09T14:16:57/ReAuction-2optRP-cihBID-BAL-LOW-final.csv",
#   "2015-12-09T17:23:37/ReAuction-2optRP-cihBID-BAL-LOW-final.csv"
#   
#   
#   
#   #"2015-12-09T17:23:37/RtCentral-CIH(GendrOF(30.0))-final.csv"
#   
#   
#           # "2015-12-04T13:51:34/ReAuction-2optRP-cihBID-final.csv",
#           # "2015-12-03T16:52:06/ReAuction-2optRP-cihBID-final.csv",
#           # "2015-12-03T16:12:53/ReAuction-2optRP-cihBID-final.csv",
#           # "2015-12-03T14:25:13/ReAuction-2optRP-cihBID-final.csv",
#           # "2015-12-02T16:53:42/ReAuction-2optRP-cihBID-OVERTIME-final.csv",
#           # "2015-12-02T16:13:32/ReAuction-2optRP-cihBID-BALANCE-final.csv",
#           # "2015-12-02T18:21:37/ReAuction-2optRP-cihBID-BAL-final.csv",
#           # "2015-12-02T18:21:37/ReAuction-2optRP-cihBID-BAL-LOW-final.csv",
#           # "2015-12-02T18:21:37/ReAuction-2optRP-cihBID-BAL-HIGH-final.csv",
#           # "2015-11-30/ReAuction-2optRP-cihBID-final.csv"
#           # "2015-11-30/RtCentral-CIH(GendrOF(30.0))-final.csv"
#           
#            )

selectData <- function(data,columns,alg_name,alg_dir){
  table <- subset(data,select=columns)
  table[,"class"] <- strtrim(table$scenario_id, 7)
  table[,"alg"] <- alg_name
  table[,"alg_dir"] <- alg_dir
  return(table)
}

plot <- function(data,name){
  
  # convert data to long-format
  melted <- melt(data,id.vars=c("scenario_id","class","alg","name"),measure.vars=c("cost","travel_time","tardiness","over_time"))
  # reorder such that appearance in data frame is used as plot order
  melted$alg <- factor(melted$alg, as.character(melted$alg))
  
  # convert data to wide-format, take average 
  means <- dcast(melted,class+alg+name~variable,fun=mean)
  
  # convert data to long format, move cost to 'wide side'
  melted_means <- melt(means,id.vars=c("class","alg", "cost", "name"),measure.vars=c("travel_time","tardiness","over_time"))
  
  means2 <- dcast(melted_means,class+alg+cost+name~variable,sum)
  
  melted_means2 <- melt(means2,id.vars=c("class","alg", "cost", "name"),measure.vars=c("travel_time","tardiness","over_time"))
  #melted_means2 <- melted_means2[order(cost),]
  
  # to sum multiple standard deviations, we average the variances and then take the square root
  sds <- dcast(melted,scenario_id+class+alg~variable,var)
  melted_sds <- melt(sds,id.vars=c("class","alg"),measure.vars=c("travel_time","tardiness","over_time"))#,measure.vars=c("travel_time_sd","tardiness_sd","over_time_sd"))
  sds2 <- dcast(melted_sds,class+alg~variable,mean)
  
  melted_sds2 <- melt(sds2,id.vars=c("class","alg"),measure.vars=c("travel_time","tardiness","over_time"))#,measure.vars=c("travel_time_sd","tardiness_sd","over_time_sd"))
  melted_means2[,"sd"] <- sqrt(melted_sds2$value)
  limits <- aes(ymax = ymax, ymin=ymin)
  

  # move the error bars to their respective positions. they need to be shifted because we are creating a stacked bar chart.
  melted_means2[,"ymax"] <- melted_means2$value + melted_means2$sd
  melted_means2[,"ymin"] <- melted_means2$value - melted_means2$sd
  
  melted_means2[melted_means2$variable=="tardiness", ] <- transform(melted_means2[melted_means2$variable=="tardiness", ],
                    ymin = ymin + melted_means2[melted_means2$variable=="travel_time","value"],
                    ymax = ymax + melted_means2[melted_means2$variable=="travel_time","value"]
  )
                    
  melted_means2[melted_means2$variable=="over_time", ] <- transform(melted_means2[melted_means2$variable=="over_time", ],
                    ymin = ymin + melted_means2[melted_means2$variable=="travel_time","value"] + melted_means2[melted_means2$variable=="tardiness","value"],
                    ymax = ymax + melted_means2[melted_means2$variable=="travel_time","value"] + melted_means2[melted_means2$variable=="tardiness","value"]
   )
  
  plot<-ggplot(melted_means2, aes(x=name,y=value,fill=variable)) + 
    geom_bar(stat='identity') + 
    geom_errorbar(stat='identity',limits) +
    labs(title=melted$class[1],y="cost",x="algorithm") +
    theme(legend.position="top") +
    scale_fill_brewer(palette="Set2") +
    theme(axis.text.x=element_text(angle = -90, hjust = 0, vjust=.5))
  ggsave(file=paste("plots/",name,".pdf",sep=""))
  return(plot)
}

alldata <- NULL
allgendreau <- NULL
for( file in files){
  table <- data.table(read.csv(paste(target.dir,file,sep="")))
  
  parts <- strsplit(file,split="/")[[1]]
  if( length(parts) > 2 ){
    stop(paste("found invalid path:",file))
  }
  print(parts)
  dir <- parts[2]
  name <- strsplit(parts[2],split="-final.csv")[1]
  name <- gsub("/", "\n", name)
  name <- gsub("RtCentral-", "", name)
  name <- gsub("ReAuction-", "", name)
  #name <- gsub("-","\n",name)
  selected_table <- selectData(table,c("cost","travel_time","tardiness","over_time","scenario_id"),name,dir)
  
  if( is.null(alldata)){
    alldata <- selected_table
  } else {
    alldata <- rbind(alldata,selected_table)
  }
  
  # gendreau data is added everytime because each file may 
  # contain some additional data that wasn't added previously
  gendreau_table <- selectData(table,c("gendr_cost","gendr_tt","gendr_tard","gendr_ot","scenario_id"),"gendreau","gendreau")
  setnames(gendreau_table,"gendr_cost","cost")
  setnames(gendreau_table,"gendr_tt","travel_time")
  setnames(gendreau_table,"gendr_tard","tardiness")
  setnames(gendreau_table,"gendr_ot","over_time")
  
  if( is.null(allgendreau)){
    allgendreau <- gendreau_table  
  } else {
    allgendreau <- rbind(allgendreau,gendreau_table)
  }
}
# filter out duplicate gendreau entries
alldata <- rbind( unique(allgendreau), alldata)

# calculate avg costs
agg <- aggregate(cost~alg+class, alldata,FUN=mean)
agg <- agg[with(agg, order(class,cost)), ]

# assign rank to each algorithm
number_of_rows <- nrow(subset(agg, class == unique(agg["class"])$class[1]))
agg[,"rank"] <- seq.int(number_of_rows)


avg_rank <- aggregate(rank~alg,agg,FUN=mean)
avg_rank <- avg_rank[with(avg_rank, order(rank)), ]
write.table(avg_rank, file="rank-table.csv", sep=",", row.names=F)

# multiply agg and sort it such that it can be merged with alldata
agg <- agg[with(agg, order(class,alg)), ]
#agg <- agg[rep(seq_len(nrow(agg)), each=5),]



# sort using same order as agg, add sum_cost and rank columns
alldata <- alldata[with(alldata, order(class,alg)), ]

alldata <- merge(alldata,agg,by=c("alg","class"))
rename(alldata, c("cost.x"="cost", "cost.y"="avg_cost"))

#break
#alldata[match(alldata$scenario_id),"sum_cost"] <- agg["cost"]
#alldata[,"rank"] <- agg["rank"]





# sort on class,sum_cost
#alldata <- alldata[with(alldata, order(class,sum_cost)), ]
# create name which is rank + alg
alldata$name <- paste( sprintf("%03d",alldata$rank),alldata$alg,sep="-")



#alldata <- subset(alldata, rank < 6 | rank == 36 | alg =="gendreau")



short_low <- subset(alldata, class=="_240_24")
short_high <-subset(alldata, class=="_240_33")
long_low <- subset(alldata, class=="_450_24")

p1 <- plot(short_low,"240_24")
p2 <- plot(short_high,"240_33")
p3 <- plot(long_low,"450_24")
multiplot(p1, p2, p3,cols=3)

