package codox;


import com.vladsch.flexmark.html.LinkResolverFactory;
import com.vladsch.flexmark.html.renderer.LinkResolverBasicContext;
import com.vladsch.flexmark.html.LinkResolver;
import clojure.lang.IFn;
import java.util.Set;


public class LinkResolverFactoryImpl implements LinkResolverFactory {
  public final Set afterDeps;
  public final Set beforeDeps;
  public final boolean affectsGlobalScope;
  public final IFn resolverFn;

  public LinkResolverFactoryImpl(Set _afterDeps, Set _beforeDeps, boolean ags,
				 IFn _resolverFn) {
    afterDeps = _afterDeps;
    beforeDeps = _beforeDeps;
    affectsGlobalScope = ags;
    resolverFn = _resolverFn;
  }

  public Set<Class<?>> getAfterDependents() { return afterDeps; }  
  public Set<Class<?>> getBeforeDependents() { return beforeDeps; }
  public boolean affectsGlobalScope() { return affectsGlobalScope; }
  public LinkResolver apply(LinkResolverBasicContext context) {
    return (LinkResolver)resolverFn.invoke(context);
  }
}
