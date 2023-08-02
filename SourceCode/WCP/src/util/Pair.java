package util;

import java.util.Objects;

public class Pair<T, U> {
	public T first;
	public U second;
	public Pair(T f, U s){
		this.first = f;
		this.second = s;
	}

	@Override
	public boolean equals(Object o){
		if (this == o)
			return true;

		if (o == null || getClass() != o.getClass())
			return false;

		Pair<?, ?> pair = (Pair<?, ?>) o;

		if (!first.equals(pair.first))
			return false;
		return second.equals(pair.second);
	}

	@Override
	public int hashCode(){
		return Objects.hash(first, second);
	}

	public String toString(){
		return "<" + first.toString() + ", " + second.toString() + ">"; 
	}
}
