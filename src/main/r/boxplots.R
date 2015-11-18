library("ggplot2")
library("data.table")

script.dir <- dirname(sys.frame(1)$ofile)
target.dir <- paste(script.dir,"/../../../files/results/latest/",sep="")

csv_files <- list.files(path=target.dir,pattern=".*-interarrivaltimes.csv")

alldata <- NULL
i <- 0
for( file in csv_files){
  print(file)
  cname <- paste("time",i,sep="")
  print(cname)
  table <- data.table(read.csv(paste(target.dir,file,sep=""),col.names=(cname)))
  
  if( is.null(alldata)){
    alldata <- table
  } else {
   # alldata <- rbind(alldata,table)
    
    alldata[,cname] <- table
  }
  
  i <- i +1
}

#file <- paste(script.dir,"/../../../files/results/latest/RtCentral-CheapestInsertionHeuristic.supplier(Gendreau06ObjectiveFunction)-_240_24-1--570648509535936272-interarrivaltimes.csv",sep="")



#dt2 <- table[,list(time=time/1000000)]


p <- ggplot(alldata, aes(factor(x), time))
p + geom_boxplot() + geom_abline(intercept = 250, slope = 0, colour ="red")


