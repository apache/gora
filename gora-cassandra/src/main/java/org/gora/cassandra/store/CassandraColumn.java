package org.gora.cassandra.store;

class CassandraColumn {
  String family;
  String superColumn;
  String column;

  public CassandraColumn(String family, String superColumn, String column) {
    this.family = family;
    this.superColumn = superColumn;
    this.column = column;
  }

  public boolean isSuperColumn() {
    return superColumn != null;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((column == null) ? 0 : column.hashCode());
    result = prime * result + ((family == null) ? 0 : family.hashCode());
    result = prime * result
        + ((superColumn == null) ? 0 : superColumn.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    CassandraColumn other = (CassandraColumn) obj;
    if (column == null) {
      if (other.column != null)
        return false;
    } else if (!column.equals(other.column))
      return false;
    if (family == null) {
      if (other.family != null)
        return false;
    } else if (!family.equals(other.family))
      return false;
    if (superColumn == null) {
      if (other.superColumn != null)
        return false;
    } else if (!superColumn.equals(other.superColumn))
      return false;
    return true;
  }
}
