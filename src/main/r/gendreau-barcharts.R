library("tikzDevice")
library("ggplot2")
library("data.table")
library("reshape2")

script.dir <- dirname(sys.frame(1)$ofile)
source(paste(script.dir,"multiplot.r",sep="/"))
target.dir <- paste(script.dir,"/../../../files/results/BEST/GENDREAU/",sep="")

files <- c(
  #"2015-12-02T19:55:45-OFFLINE/Central-Opt2Bfs(GendrOF(30.0))-final.csv",
  "2015-12-04T16:03:02/RtCentral-Opt2BfsRT(GendrOF(30.0))-final.csv",
  "2015-12-09T17:23:37/RtCentral-Opt2BfsRT(GendrOF(30.0))-final.csv",
  
  "2015-12-04T16:03:02/ReAuction-2optRP-cihBID-final.csv",
   #"2015-11-30/RtCentral-Opt2BfsRT(GendrOF(30.0))-final.csv",
  #"2015-12-04T15:12:52/ReAuction-2optRP-cihBID-final.csv",
  "2015-12-09T11:42:04/ReAuction-2optRP-cihBID-final.csv",
  "2015-12-09T14:16:57/ReAuction-2optRP-cihBID-final.csv",
  "2015-12-09T17:23:37/ReAuction-2optRP-cihBID-final.csv",
  
  "2015-12-09T14:16:57/ReAuction-2optRP-cihBID-BAL-HIGH-final.csv",
  "2015-12-09T11:42:04/ReAuction-2optRP-cihBID-BAL-HIGH-final.csv",
  "2015-12-09T17:23:37/ReAuction-2optRP-cihBID-BAL-HIGH-final.csv",
  
  "2015-12-09T11:42:04/ReAuction-2optRP-cihBID-BAL-LOW-final.csv",
  "2015-12-09T14:16:57/ReAuction-2optRP-cihBID-BAL-LOW-final.csv",
  "2015-12-09T17:23:37/ReAuction-2optRP-cihBID-BAL-LOW-final.csv"
  
  
  
  #"2015-12-09T17:23:37/RtCentral-CIH(GendrOF(30.0))-final.csv"
  
  
          # "2015-12-04T13:51:34/ReAuction-2optRP-cihBID-final.csv",
          # "2015-12-03T16:52:06/ReAuction-2optRP-cihBID-final.csv",
          # "2015-12-03T16:12:53/ReAuction-2optRP-cihBID-final.csv",
          # "2015-12-03T14:25:13/ReAuction-2optRP-cihBID-final.csv",
          # "2015-12-02T16:53:42/ReAuction-2optRP-cihBID-OVERTIME-final.csv",
          # "2015-12-02T16:13:32/ReAuction-2optRP-cihBID-BALANCE-final.csv",
          # "2015-12-02T18:21:37/ReAuction-2optRP-cihBID-BAL-final.csv",
          # "2015-12-02T18:21:37/ReAuction-2optRP-cihBID-BAL-LOW-final.csv",
          # "2015-12-02T18:21:37/ReAuction-2optRP-cihBID-BAL-HIGH-final.csv",
          # "2015-11-30/ReAuction-2optRP-cihBID-final.csv"
          # "2015-11-30/RtCentral-CIH(GendrOF(30.0))-final.csv"
          
           )

selectData <- function(data,columns,alg_name){
  table <- subset(data,select=columns)
  table[,"class"] <- strtrim(table$scenario_id, 7)
  table[,"alg"] <- alg_name
  return(table)
}

plot <- function(data,name){
  melted <- melt(data,id.vars=c("scenario_id","class","alg"),measure.vars=c("travel_time","tardiness","over_time"))
  # reorder such that appearance in data frame is used as plot order
  melted$alg2 <- factor(melted$alg, as.character(melted$alg))
  
  means <- dcast(melted,scenario_id+class+alg2~variable,mean)
  melted_means <- melt(means,id.vars=c("class","alg2"),measure.vars=c("travel_time","tardiness","over_time"))
  means2 <- dcast(melted_means,class+alg2~variable,sum)
  melted_means2 <- melt(means2,id.vars=c("class","alg2"),measure.vars=c("travel_time","tardiness","over_time"))
  
  # to sum multiple standard deviations, we average the variances and then take the square root
  sds <- dcast(melted,scenario_id+class+alg2~variable,var)
  melted_sds <- melt(sds,id.vars=c("class","alg2"),measure.vars=c("travel_time","tardiness","over_time"))#,measure.vars=c("travel_time_sd","tardiness_sd","over_time_sd"))
  sds2 <- dcast(melted_sds,class+alg2~variable,mean)
  melted_sds2 <- melt(sds2,id.vars=c("class","alg2"),measure.vars=c("travel_time","tardiness","over_time"))#,measure.vars=c("travel_time_sd","tardiness_sd","over_time_sd"))
  
  melted_means2[,"sd"] <- sqrt(melted_sds2$value)
  #total <- merge(melted_means,melted_sds,by=c("class","alg2","variable"),suffixes=c(".mean",".sd"))
#  print(melted_means)
 
#  melted_total <-  melt(total,id.vars=c("scenario_id","alg2"),measure.vars=c("travel_time","tardiness","over_time","travel_time_sd","tardiness_sd","over_time_sd"))
  
  print(melted_means2)
  limits <- aes(ymax = ymax, ymin=ymin)
  print(limits)
  #limits[variable=="tardiness"]
  
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
  
  plot<-ggplot(melted_means2, aes(x=alg2,y=value,fill=variable)) + 
    geom_bar(stat='identity') + 
    geom_errorbar(stat='identity',limits) +
    labs(title=melted$class[1],y="cost",x="algorithm") +
    theme(legend.position="top") +
    scale_fill_brewer(palette="Set2") +
    theme(axis.text.x=element_text(angle = -90, hjust = 0, vjust=.5))
  ggsave(file=paste(name,".pdf",sep=""))
  return(plot)
}

alldata <- NULL
for( file in files){
  table <- data.table(read.csv(paste(target.dir,file,sep="")))
  name <- strsplit(file,split="-final.csv")[1]
  name <- gsub("/", "\n", name)
  name <- gsub("RtCentral-", "", name)
  name <- gsub("ReAuction-", "", name)
  #name <- gsub("-","\n",name)
  selected_table <- selectData(table,c("travel_time","tardiness","over_time","scenario_id"),name)
  
  if( is.null(alldata)){
    gendreau_table <- selectData(table,c("gendr_tt","gendr_tard","gendr_ot","scenario_id"),"gendreau")
    setnames(gendreau_table,"gendr_tt","travel_time")
    setnames(gendreau_table,"gendr_tard","tardiness")
    setnames(gendreau_table,"gendr_ot","over_time")
    
    alldata <- rbind(gendreau_table,selected_table)
  } else {
    alldata <- rbind(alldata,selected_table)
  }
}

short_low <- subset(alldata, class=="_240_24")
short_high <-subset(alldata, class=="_240_33")
long_low <- subset(alldata, class=="_450_24")

p1 <- plot(short_low,"240_24")
p2 <- plot(short_high,"240_33")
p3 <- plot(long_low,"450_24")
multiplot(p1, p2, p3,cols=3)

