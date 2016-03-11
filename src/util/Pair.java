package util;



public class Pair<T1,T2> {
	public T1 m_a;
	public T2 m_b;

	public Pair(T1 a,T2 b) {
		m_a = a;
		m_b = b;
	}   
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Pair<?,?>)){
			return false;
		}
		Pair<T1,T2> p=(Pair<T1,T2>)obj;
		return m_a.equals(p.m_a)&&m_b.equals(p.m_b);
	}
	public String toString() {
		return "<" + m_a + "," + m_b + ">";
	}
}
