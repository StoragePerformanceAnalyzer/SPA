/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package edu.kit.sdq.storagebenchmarkharness.SBHModel;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.EObjectResolvingEList;
import edu.kit.sdq.storagebenchmarkharness.emf.EObjectResolvingNonCheckingEList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Dependent Variables</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables#getBenchmarkPrefix <em>Benchmark Prefix</em>}</li>
 *   <li>{@link edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables#getValues <em>Values</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class DependentVariables extends EObjectImpl {
	/**
	 * The default value of the '{@link #getBenchmarkPrefix() <em>Benchmark Prefix</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBenchmarkPrefix()
	 * @generated
	 * @ordered
	 */
	protected static final String BENCHMARK_PREFIX_EDEFAULT = "";

	/**
	 * The cached value of the '{@link #getBenchmarkPrefix() <em>Benchmark Prefix</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBenchmarkPrefix()
	 * @generated
	 * @ordered
	 */
	protected String benchmarkPrefix = BENCHMARK_PREFIX_EDEFAULT;

	/**
	 * The cached value of the '{@link #getValues() <em>Values</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getValues()
	 * @generated
	 * @ordered
	 */
	protected EList<DependentVariablesValue> values;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected DependentVariables() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return SBHModelPackage.Literals.DEPENDENT_VARIABLES;
	}

	/**
	 * Returns the value of the '<em><b>Benchmark Prefix</b></em>' attribute.
	 * The default value is <code>""</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Benchmark Prefix</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Benchmark Prefix</em>' attribute.
	 * @see #setBenchmarkPrefix(String)
	 * @generated
	 */
	public String getBenchmarkPrefix() {
		return benchmarkPrefix;
	}

	/**
	 * Sets the value of the '{@link edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables#getBenchmarkPrefix <em>Benchmark Prefix</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Benchmark Prefix</em>' attribute.
	 * @see #getBenchmarkPrefix()
	 * @generated
	 */
	public void setBenchmarkPrefix(String newBenchmarkPrefix) {
		String oldBenchmarkPrefix = benchmarkPrefix;
		benchmarkPrefix = newBenchmarkPrefix;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SBHModelPackage.DEPENDENT_VARIABLES__BENCHMARK_PREFIX, oldBenchmarkPrefix, benchmarkPrefix));
	}

	/**
	 * Returns the value of the '<em><b>Values</b></em>' reference list.
	 * The list contents are of type {@link edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValue}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Values</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Values</em>' reference list.
	 * @generated NOT
	 */
	public List<DependentVariablesValue> getValues() {
		if (values == null) {
			values = new EObjectResolvingNonCheckingEList<DependentVariablesValue>(DependentVariablesValue.class, this, SBHModelPackage.DEPENDENT_VARIABLES__VALUES);
		}
		return values;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case SBHModelPackage.DEPENDENT_VARIABLES__BENCHMARK_PREFIX:
				return getBenchmarkPrefix();
			case SBHModelPackage.DEPENDENT_VARIABLES__VALUES:
				return getValues();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case SBHModelPackage.DEPENDENT_VARIABLES__BENCHMARK_PREFIX:
				setBenchmarkPrefix((String)newValue);
				return;
			case SBHModelPackage.DEPENDENT_VARIABLES__VALUES:
				getValues().clear();
				getValues().addAll((Collection<? extends DependentVariablesValue>)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case SBHModelPackage.DEPENDENT_VARIABLES__BENCHMARK_PREFIX:
				setBenchmarkPrefix(BENCHMARK_PREFIX_EDEFAULT);
				return;
			case SBHModelPackage.DEPENDENT_VARIABLES__VALUES:
				getValues().clear();
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case SBHModelPackage.DEPENDENT_VARIABLES__BENCHMARK_PREFIX:
				return BENCHMARK_PREFIX_EDEFAULT == null ? benchmarkPrefix != null : !BENCHMARK_PREFIX_EDEFAULT.equals(benchmarkPrefix);
			case SBHModelPackage.DEPENDENT_VARIABLES__VALUES:
				return values != null && !values.isEmpty();
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (benchmarkPrefix: ");
		result.append(benchmarkPrefix);
		result.append(')');
		return result.toString();
	}

} // DependentVariables
