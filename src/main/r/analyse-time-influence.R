library("ggplot2")
library("data.table")

script.dir <- dirname(sys.frame(1)$ofile)
source(paste(script.dir,"multiplot.r",sep="/"))
target.dir <- paste(script.dir,"/../../../files/results/BEST/TIME_DEVIATION/2015-11-26T22:59:39/",sep="")

files <- c("ReAuction-2optRP-cihBID-final.csv",
           "RtCentral-CIH(GendrOF(50.0))-final.csv",
           "RtCentral-Opt2BfsRT(GendrOF(50.0))-final.csv")

plots = list()
for( file in files){
  table <- data.table(read.csv(paste(target.dir,file,sep="")))
  
  p <- ggplot(table, aes(x=scenario_id, cost)) 
  p <- p +
    geom_boxplot() + 
    ylab("cost") + 
    labs(title=file) +
    expand_limits(y = c(0,20100)) +
    theme(axis.text.x=element_text(angle = -90, hjust = 0, vjust=.5))
  
  plots <- c(plots,list(p))
}

show(plots[3])
multiplot(plotlist=plots,cols=3)




