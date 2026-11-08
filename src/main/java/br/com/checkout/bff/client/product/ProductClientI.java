package br.com.checkout.bff.client.product;

import java.util.List;

/** Porta do BFF para o serviço de produtos. */
public interface ProductClientI {

    /** Catálogo completo de planos à venda. */
    List<CatalogProduct> findAll();
}
