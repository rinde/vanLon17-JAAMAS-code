library("tikzDevice")
library("ggplot2")
library("data.table")
library("reshape2")


#
# ggplot objects can be passed in ..., or to plotlist (as a list of ggplot objects)
# - cols:   Number of columns in layout
# - layout: A matrix specifying the layout. If present, 'cols' is ignored.
#
# If the layout is something like matrix(c(1,2,3,3), nrow=2, byrow=TRUE),
# then plot 1 will go in the upper left, 2 will go in the upper right, and
# 3 will go all the way across the bottom.
#
multiplot <- function(..., plotlist=NULL, file, cols=1, layout=NULL) {
  library(grid)
  
  # Make a list from the ... arguments and plotlist
  plots <- c(list(...), plotlist)
  
  numPlots = length(plots)
  
  # If layout is NULL, then use 'cols' to determine layout
  if (is.null(layout)) {
    # Make the panel
    # ncol: Number of columns of plots
    # nrow: Number of rows needed, calculated from # of cols
    layout <- matrix(seq(1, cols * ceiling(numPlots/cols)),
                     ncol = cols, nrow = ceiling(numPlots/cols))
  }
  
  if (numPlots==1) {
    print(plots[[1]])
    
  } else {
    # Set up the page
    grid.newpage()
    pushViewport(viewport(layout = grid.layout(nrow(layout), ncol(layout))))
    
    # Make each plot, in the correct location
    for (i in 1:numPlots) {
      # Get the i,j matrix positions of the regions that contain this subplot
      matchidx <- as.data.frame(which(layout == i, arr.ind = TRUE))
      
      print(plots[[i]], vp = viewport(layout.pos.row = matchidx$row,
                                      layout.pos.col = matchidx$col))
    }
  }
}

script.dir <- dirname(sys.frame(1)$ofile)
target.dir <- paste(script.dir,"/../../../files/results/BEST/GENDREAU/2015-11-30/",sep="")

files <- c("ReAuction-2optRP-cihBID-final.csv",
           "RtCentral-CIH(GendrOF(30.0))-final.csv",
           "RtCentral-Opt2BfsRT(GendrOF(30.0))-final.csv")

selectData <- function(data,columns,alg_name){
  table <- subset(data,select=columns)
  table[,"class"] <- strtrim(table$scenario_id, 7)
  table[,"alg"] <- alg_name
  return(table)
}

plot <- function(data,name){
  melted <- melt(data,id.vars=c("scenario_id","class","alg"),measure.vars=c("travel_time","tardiness","over_time"))
  plot<-ggplot(melted, aes(x=alg,y=value,fill=variable)) + geom_bar(stat='identity') + labs(title=melted$class[1],y="cost",x="algorithm")
  ggsave(file=paste(name,".pdf",sep=""))
  return(plot)
}

alldata <- NULL
for( file in files){
  table <- data.table(read.csv(paste(target.dir,file,sep="")))
  name <- strsplit(file,split="-final.csv")[1]
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
multiplot(p1, p2, p3)

