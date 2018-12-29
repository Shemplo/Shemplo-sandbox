library(GEOquery)
library(limma)
library(org.Hs.eg.db)
library(data.table)
library(igraph)
source("https://raw.githubusercontent.com/assaron/r-utils/master/R/exprs.R")

GSE3189_ann <- getGEO("GSE3189", AnnotGPL = TRUE, destdir = "data")
es <- GSE3189_ann$GSE3189_series_matrix.txt.gz
es <- es[nchar(fData(es)$`Gene ID`) != 0]
es <- es[-grep("///", fData(es)$`Gene ID`)]
#es <- es[, pData(es)$characteristics_ch1 != "Normal"]

if (file.exists ("trainset.csv")) {
  train <- read.csv("trainset.csv")
  train
  es <- es[, pData(es)$geo_accession %in% train$geo_access]
}

#collapse similar Gene IDs
es <- collapseBy(es, fData(es)$`Gene ID`, median)

fData(es)$symbol <- mapIds(org.Hs.eg.db, keys = rownames(es), column = "SYMBOL", keytype = "ENTREZID")
# make expressionSet object
es$condition <- as.character (pData(es)$`characteristics_ch1`)

##### differential expression
es.design <- model.matrix(~0+condition, data=pData(es))

fit <- lmFit(es, es.design)

fit2 <- contrasts.fit(fit, makeContrasts(conditionMelanoma-conditionNevus,
                                         levels=es.design))
fit2 <- eBayes(fit2)
de <- normalizeGeneDE(topTable(fit2, adjust.method="BH", number=Inf))
de <- de[order(pval), ]

##### network
library(BioNet)
gt <- fread("data/BINARY_PROTEIN_PROTEIN_INTERACTIONS.txt")[, list(from=V1, to=V4)]
g <- graph_from_data_frame(gt, directed=FALSE)
g <- simplify(largestComp(induced.subgraph(g, V(g)$name %in% de$Gene.symbol)))
V(g)$geneSymbol <- V(g)$name
V(g)$pval <- de[match(V(g)$name, Gene.symbol), pval]

library (mcmcRanking)
g <- set_likelihood (g, 1e-7)
x <- mcmc_sample(g, subgraph_order = 200, times = 10, niter = 100000)
freq <- get_frequency(x, prob = TRUE)
#write.csv(sort (freq, decreasing = TRUE), file = "freqs.csv")
freq <- sort (freq, decreasing = TRUE)
xy <- data.frame (gene = names (freq), frequency = freq)
write.table (xy, file = "freqs.csv", sep = ";", dec = ".",
             quote = FALSE, row.names = FALSE)
