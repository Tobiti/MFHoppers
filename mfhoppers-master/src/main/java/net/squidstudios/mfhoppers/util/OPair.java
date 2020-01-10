package net.squidstudios.mfhoppers.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OPair<F, S> {

    private F first;
    private S second;

}
