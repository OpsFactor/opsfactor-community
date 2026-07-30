package com.opsfactor.community.capability.cluster.domain.produto;

import com.opsfactor.community.platform.utility.Constantes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.io.Serializable;

import lombok.NonNull;

@Data @Entity
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(of = "regraAlocacaoClusterProdutosStatusCompositeKey")
public class RegraAlocacaoClusterProdutosStatus implements Serializable {

    @EmbeddedId
    private RegraAlocacaoClusterProdutosStatusCompositeKey regraAlocacaoClusterProdutosStatusCompositeKey;

    @Embeddable 
    @Data 
    @NoArgsConstructor 
    @AllArgsConstructor 
    @EqualsAndHashCode
    public static class RegraAlocacaoClusterProdutosStatusCompositeKey implements Serializable {

        @ManyToOne(optional = false)
        @NonNull
        private RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutos;
        
        @Enumerated(EnumType.STRING)
        @NonNull
        private Constantes.StatusProduto statusProduto;    
    }
    
    public void setStatusProduto(Constantes.StatusProduto statusProduto) {
        regraAlocacaoClusterProdutosStatusCompositeKey.setStatusProduto(statusProduto);
    }
    
    public void setRegraAlocacaoClusterProdutos(RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutos) {
        getRegraAlocacaoClusterProdutosStatusCompositeKey().regraAlocacaoClusterProdutos = regraAlocacaoClusterProdutos;
    }
    
    public Constantes.StatusProduto getStatusProduto() {
        return regraAlocacaoClusterProdutosStatusCompositeKey.getStatusProduto();
    }

    public RegraAlocacaoClusterProdutosStatusCompositeKey getCompositeKey(){
        if (regraAlocacaoClusterProdutosStatusCompositeKey == null) regraAlocacaoClusterProdutosStatusCompositeKey = new RegraAlocacaoClusterProdutosStatusCompositeKey();
        return regraAlocacaoClusterProdutosStatusCompositeKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegraAlocacaoClusterProdutosStatus)) return false;
        RegraAlocacaoClusterProdutosStatus that = (RegraAlocacaoClusterProdutosStatus) o;
        return getCompositeKey().equals(that.getCompositeKey());
    }

    @Override
    public int hashCode() {
        return getCompositeKey().hashCode();
    }
    
    public RegraAlocacaoClusterProdutos getRegraAlocacaoClusterProdutos() {
        return regraAlocacaoClusterProdutosStatusCompositeKey.getRegraAlocacaoClusterProdutos();
    }
    
}
