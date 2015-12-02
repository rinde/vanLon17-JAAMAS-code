library("tikzDevice")
library("ggplot2")
library("data.table")

script.dir <- dirname(sys.frame(1)$ofile)
target.dir <- paste(script.dir,"/../../../files/results/BEST/GENDREAU/2015-11-30/time-deviations/",sep="")

csv_files <- list.files(path=target.dir,pattern=".*-interarrivaltimes.csv")
alldata <- NULL
allsummary <- NULL
for( file in csv_files){
  parts <- unlist(strsplit(file,"-"))
  size <- length(parts)
  
  table <- data.table(read.csv(paste(target.dir,file,sep=""),col.names=("time")))
  table <- table[,list(time=(time/1000000)-250)]
  
  # if seed is negative (contains a '-') we will find an empty part at this position
  if( parts[size-2] == ''){
    seed <- paste("-",parts[size-1],sep="")
    size <- size-1
  }else {
    seed <- parts[size-1]
  }
  instanceId <- parts[size-2]
  problemClass <- gsub("_","-",parts[size-3])
  
  
  configString <- paste(parts[1:(size-4)],collapse='-')
  
  if(grepl("Opt2",configString)){
    config <- "Opt2"
  }else if(grepl("CheapestInsertionHeuristic", configString)){
    config <- "CIH"
  } else {
    config <- paste(parts[1:(size-4)],collapse='-')
  }
  
  table[,"seed"] <- seed
  table[,"instanceId"] <- instanceId
  table[,"problemClass"] <- problemClass
  table[,"config"] <- config
  
  # sum all positives and sum all negatives
  negatives <- subset(table,time < -1)
  positives <- subset(table,time > 1)
  summary <- data.table(seed,instanceId,problemClass,config,
               mean(table$time),    sum(table$time),    length(table$time),
               mean(negatives$time),sum(negatives$time),length(negatives$time),
               mean(positives$time),sum(positives$time),length(positives$time)
               )
  names(summary) <- c("seed","instanceId","problemClass","config","mean","sum","length","mean-","sum-","length-","mean+","sum+","length+")
  
  if( is.null(alldata)){
    alldata <- table
    
    allsummary <- summary
  } else {
    alldata <- rbind(alldata,table)
    allsummary <- rbind(allsummary,summary)
  }
}

#alldata <- subset(alldata, problemClass == "-450-24" & instanceId == "1")
tikz(file="boxplot.tex",standAlone=T)#,height=5,width=5)#8.3,width=11.6)
p <- ggplot(alldata, aes(x=interaction(config,problemClass), time)) 
p <- p +geom_boxplot(outlier.size=1,outlier.colour="orange") + 
  geom_abline(intercept = 0, slope = 0, colour ="red") + 
  ylab("time (ms)")

show(p)
garbage <- dev.off()   


