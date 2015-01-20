package edu.kit.sdq.storagebenchmarkharness.emf;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EObjectResolvingEList;

public class EObjectResolvingNonCheckingEList<E> extends EObjectResolvingEList<E>
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7840655762718343348L;
	
	public EObjectResolvingNonCheckingEList(Class<?> dataClass, InternalEObject owner, int featureId)
	{
		super(dataClass, owner, featureId);
	}
	
	@Override
	protected boolean isUnique()
	{
		return false;
	}
}
