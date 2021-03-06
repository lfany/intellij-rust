package org.rust.lang.core.resolve

import com.intellij.openapi.project.Project
import org.rust.lang.core.psi.RsImplItem
import org.rust.lang.core.psi.RsTraitItem
import org.rust.lang.core.psi.ext.queryAttributes
import org.rust.lang.core.psi.ext.resolveToTrait
import org.rust.lang.core.resolve.indexes.RsImplIndex
import org.rust.lang.core.types.ty.Ty
import org.rust.lang.core.types.ty.findImplsAndTraits
import org.rust.lang.core.types.infer.remapTypeParameters
import org.rust.lang.core.types.type
import org.rust.lang.core.types.ty.TyUnknown


fun findDerefTarget(project: Project, ty: Ty): Ty? {
    val impls = RsImplIndex.findImpls(project, ty)
    for (impl in impls) {
        val trait = impl.traitRef?.resolveToTrait ?: continue
        if (!trait.isDeref) continue
        return lookupAssociatedType(impl, "Target")
    }
    return null
}

fun findIteratorItemType(project: Project, ty: Ty): Ty {
    val impl = findImplsAndTraits(project, ty).first
        .find {
            val traitName = it.traitRef?.resolveToTrait?.name
            traitName == "Iterator" || traitName == "IntoIterator"
        } ?: return TyUnknown

    val rawType = lookupAssociatedType(impl, "Item")
    val typeParameterMap = impl.remapTypeParameters(ty.typeParameterValues)
    return rawType.substitute(typeParameterMap)
}


private val RsTraitItem.langAttribute: String? get() {
    if (this.stub != null) return this.stub.langAttribute
    return this.queryAttributes.langAttribute
}

private val RsTraitItem.isDeref: Boolean get() = langAttribute == "deref"

private fun lookupAssociatedType(impl: RsImplItem, name: String): Ty =
    impl.typeAliasList.find { it.name == name }?.typeReference?.type
        ?: TyUnknown
