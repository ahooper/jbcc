// JLS §10.7
@SuppressWarnings("serial")
class Array<T> implements Cloneable, java.io.Serializable {
	public final int length = 0;
	@SuppressWarnings("unchecked")
	public T[] clone() {
		try {
			return (T[])super.clone(); // unchecked warning
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.getMessage());
		}
	}
}
