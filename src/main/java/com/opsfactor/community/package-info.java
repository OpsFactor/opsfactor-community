/**
 * Package raiz da edicao OpsFactor Community.
 *
 * <p>Este modulo ancora o root tecnico do backend Community. Durante a migracao,
 * os módulos funcionais legados ainda ficam sob subpacotes Community, como
 * {@code com.opsfactor.community.capability}, {@code com.opsfactor.community.platform.security},
 * {@code com.opsfactor.community.platform.scheduler} e {@code com.opsfactor.community.platform.rinstance},
 * permitindo recortes incrementais sem dependência inversa para Enterprise.</p>
 *
 * <p>A taxonomia de destino usa {@code capability}, {@code platform} e
 * {@code web}, de forma análoga ao Enterprise. A migração ocorre em blocos
 * coesos e não altera regra funcional junto com o movimento de package.</p>
 */
package com.opsfactor.community;
